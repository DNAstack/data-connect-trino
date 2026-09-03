package com.dnastack.ga4gh.dataconnect.controller;

import com.dnastack.audit.aspect.AuditActionUri;
import com.dnastack.audit.aspect.AuditEventCustomize;
import com.dnastack.audit.aspect.AuditIgnore;
import com.dnastack.audit.aspect.AuditIgnoreHeaders;
import com.dnastack.ga4gh.dataconnect.adapter.shared.QueryJobAppenderAuditEventCustomizer;
import com.dnastack.ga4gh.dataconnect.adapter.trino.DataConnectRequest;
import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoDataConnectAdapter;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.TableApiErrorException;
import com.dnastack.ga4gh.dataconnect.model.TableData;
import com.dnastack.tenancy.context.TenantIdentitySource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class DataConnectController {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * The tenant-scoped form of every path here. The un-prefixed form is kept alongside it and resolves to the
     * management tenant, so callers that predate tenancy keep working.
     */
    static final String TENANT_PREFIX = "/tenants/{" + TenantIdentitySource.TENANT_PATH_VARIABLE + "}";

    /** What follows this service's own prefix on a /search/** path: the page Trino issued. */
    private static final Pattern RELAYED_PAGE = Pattern.compile("^(?:/tenants/[^/]+)?/search/(.+)$");

    private final TrinoDataConnectAdapter trinoDataConnectAdapter;

    private static final RetryConfig retryConfig = RetryConfig.<TableData>custom()
        .intervalFunction(IntervalFunction.of(1)) // trino throttles us for up to 10 seconds per page request when no further results are ready
        .maxAttempts(4) // first page is always very quick; next 3 tries will take maximum 30s
        .retryOnResult(tableData ->
            tableData.getPagination() != null
            && tableData.getPagination().getNextPageUrl() != null
            && (tableData.getData() == null || tableData.getData().isEmpty()))
        .retryOnException(e -> false)
        .build();
    private static final RetryRegistry retryRegistry = RetryRegistry.of(retryConfig);

    @Autowired
    public DataConnectController(TrinoDataConnectAdapter trinoDataConnectAdapter) {
        this.trinoDataConnectAdapter = trinoDataConnectAdapter;
    }

    /**
     * <a href="https://github.com/ga4gh-discovery/data-connect/blob/develop/SPEC.md#search">Data Connect Search</a>
     *
     * <p>
     * This endpoint may cause problems for clients with connection timeouts set to less than 20 seconds.
     * It waits for the job execution for a maximum of 20 seconds before returning a response. If the job
     * is still queued after this time, it falls back to returning a response for the queued job.
     * Clients with shorter connection timeouts might experience timeouts before receiving a response.
     * </p>
     *
     *
     * @param dataConnectRequest query to be processed by Trino
     * @param request incoming HTTP request
     * @param clientSuppliedCredentials extra credentials required by Trino
     * @return TableData object which consists of data model, data or errors, pagination and query job information
     */
    @AuditActionUri("data-connect:search")
    @AuditIgnoreHeaders("GA4GH-Search-Authorization")
    @AuditEventCustomize(QueryJobAppenderAuditEventCustomizer.class)
    @PreAuthorize("@accessEvaluator.canAccessTenantResource('/search', {'data-connect:query', 'data-connect:data'}, {'data-connect:query', 'data-connect:data'})")
    @PostMapping(value = {"/search", TENANT_PREFIX + "/search"})
    public TableData search(@RequestBody DataConnectRequest dataConnectRequest,
                            HttpServletRequest request,
                            @AuditIgnore @RequestHeader(value = "GA4GH-Search-Authorization", defaultValue = "") List<String> clientSuppliedCredentials) {

        try {
            log.debug("Request: /search query= {}", dataConnectRequest.getSqlQuery());
            final TableData tableData = trinoDataConnectAdapter
                .search(dataConnectRequest.getSqlQuery(), request, parseCredentialsHeader(clientSuppliedCredentials), null);

            // Motivation for the following code is to resolve auth errors in Trino on the POST request rather than during subsequent GET requests.
            // If the Trino query job is not executed within the given limit (~16 seconds) it falls back to return current response.
            // see https://github.com/DNAstack/data-connect-trino/pull/45
            final Retry retry = retryRegistry.retry("search");
            final Supplier<TableData> tableDataSupplier = Retry.decorateSupplier(retry, new Supplier<>() {
                    TableData previousPage = tableData;
                    @Override
                    public TableData get() {
                        TableData nextSearchPage = trinoDataConnectAdapter.getNextSearchPage(
                            relayedPagePath(previousPage.getPagination().getNextPageUrl().getPath(), request.getContextPath()),
                            previousPage.getQueryJob().getId(),
                            request,
                            parseCredentialsHeader(clientSuppliedCredentials));

                        previousPage = nextSearchPage;
                        return nextSearchPage;
                    }
                }

            );
            return tableDataSupplier.get();
        } catch (Exception ex) {
            throw new TableApiErrorException(ex, TableData::errorInstance);
        }

    }

    @AuditActionUri("data-connect:next-page")
    @AuditIgnoreHeaders("GA4GH-Search-Authorization")
    @AuditEventCustomize(QueryJobAppenderAuditEventCustomizer.class)
    @PreAuthorize("@accessEvaluator.canAccessTenantResource('/search/', {'data-connect:query', 'data-connect:data'}, {'data-connect:query', 'data-connect:data'})")
    @GetMapping(value = {"/search/**", TENANT_PREFIX + "/search/**"})
    public TableData getNextPaginatedResponse(@RequestParam("queryJobId") String queryJobId,
                                              HttpServletRequest request,
                                              @AuditIgnore @RequestHeader(value = "GA4GH-Search-Authorization", defaultValue = "") List<String> clientSuppliedCredentials) {
        String page = relayedPagePath(request.getRequestURI(), request.getContextPath());
        log.debug("Request: /search/** page= {}", page);
        TableData tableData;

        try {
            tableData = trinoDataConnectAdapter
                .getNextSearchPage(page, queryJobId, request, parseCredentialsHeader(clientSuppliedCredentials));
        } catch (Exception ex) {
            throw new TableApiErrorException(ex, TableData::errorInstance);
        }

        if(log.isDebugEnabled()) {
            String tableDataLength = "NULL";
            if (tableData.getData() != null) {
                tableDataLength = String.valueOf(tableData.getData().size());
            }

            String nextURL = "NULL";
            String trinoNextURL = "NULL";
            if (tableData.getPagination() != null) {
                nextURL = (tableData.getPagination().getNextPageUrl() == null)
                          ? "null"
                          : tableData.getPagination().getNextPageUrl().toString();
                trinoNextURL = (tableData.getPagination().getTrinoNextPageUrl() == null)
                                ? "null"
                                : tableData.getPagination().getTrinoNextPageUrl().toString();
            }

            if(log.isTraceEnabled()) {
                try {
                    String json = objectMapper.writeValueAsString(tableData);
                    log.debug("Returning " + tableDataLength + " rows with nextURL=" + nextURL + " and trinoNextURL=" + trinoNextURL + " json: " + json);
                } catch (JsonProcessingException e) {
                    log.error("Error producing debug log output ", e);
                }
            }else{
                log.debug("Returning " + tableDataLength + " rows with nextURL=" + nextURL + " and trinoNextURL=" + trinoNextURL);
            }
        }
        return tableData;
    }

    @AuditActionUri("data-connect:delete-query")
    @AuditIgnoreHeaders("GA4GH-Search-Authorization")
    @AuditEventCustomize(QueryJobAppenderAuditEventCustomizer.class)
    @PreAuthorize("@accessEvaluator.canAccessTenantResource('/search/', {'data-connect:query'}, {'data-connect:query'})")
    @DeleteMapping(value = {"/search/**", TENANT_PREFIX + "/search/**"})
    public ResponseEntity<?> deleteSearchQuery(@RequestParam("queryJobId") String queryJobId,
                                               HttpServletRequest request,
                                               @AuditIgnore @RequestHeader(value = "GA4GH-Search-Authorization", defaultValue = "") List<String> clientSuppliedCredentials) {
        // A request that names no page at all leaves the page empty, which the adapter rejects like any other
        // page that does not belong to this query job.
        String page = relayedPagePath(request.getRequestURI(), request.getContextPath());
        log.info("Terminating query with ID: {}", queryJobId);
        try {
            trinoDataConnectAdapter.deleteQueryJob(page, queryJobId, parseCredentialsHeader(clientSuppliedCredentials));
        } catch (Exception ex) {
            // Carries the status the exception asks for, and the error body a GET of the same page would return.
            throw new TableApiErrorException(ex, TableData::errorInstance);
        }
        return ResponseEntity.noContent().build();
    }

    /**
     * The Trino page a {@code /search/**} path addresses, which is everything after this service's own prefix:
     * the context path, the optional {@code /tenants/{tenantId}} segment, and {@code /search/}.
     *
     * @param path        an absolute path on this service, either a request URI or the path of a page URL it generated
     * @param contextPath the servlet context path the service is mounted under, possibly empty
     * @return the page path, or an empty string if the given path addresses no page
     */
    static String relayedPagePath(String path, String contextPath) {
        Matcher matcher = RELAYED_PAGE.matcher(path.substring(contextPath.length()));
        return matcher.matches() ? matcher.group(1) : "";
    }

    // TODO make this method into a Spring MVC parameter provider
    public static Map<String, String> parseCredentialsHeader(List<String> clientSuppliedCredentials) {
        return clientSuppliedCredentials.stream()
            .map(val -> val.split("=", 2))
            .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));
    }

}
