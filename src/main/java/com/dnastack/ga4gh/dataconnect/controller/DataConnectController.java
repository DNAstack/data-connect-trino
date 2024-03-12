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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class DataConnectController {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final TrinoDataConnectAdapter trinoDataConnectAdapter;

    @Autowired
    public DataConnectController(TrinoDataConnectAdapter trinoDataConnectAdapter) {
        this.trinoDataConnectAdapter = trinoDataConnectAdapter;
    }

    @AuditActionUri("data-connect:search")
    @AuditIgnoreHeaders("GA4GH-Search-Authorization")
    @AuditEventCustomize(QueryJobAppenderAuditEventCustomizer.class)
    @PreAuthorize("hasAuthority('SCOPE_data-connect:query') && hasAuthority('SCOPE_data-connect:data') && @accessEvaluator.canAccessResource('/search', {'data-connect:query', 'data-connect:data'}, {'data-connect:query', 'data-connect:data'})")
    @PostMapping(value = "/search")
    public TableData search(@RequestBody DataConnectRequest dataConnectRequest,
                            HttpServletRequest request,
                            @AuditIgnore @RequestHeader(value = "GA4GH-Search-Authorization", defaultValue = "") List<String> clientSuppliedCredentials) {
        TableData tableData;

        try {
            log.debug("Request: /search query= {}", dataConnectRequest.getSqlQuery());
            tableData = trinoDataConnectAdapter
                .search(dataConnectRequest.getSqlQuery(), request, parseCredentialsHeader(clientSuppliedCredentials), null);
            while (tableData.getPagination().getNextPageUrl().toString().contains("queued")) {
                tableData = trinoDataConnectAdapter.getNextSearchPage(
                    tableData.getPagination().getNextPageUrl().getPath().split(request.getContextPath() + "/search/")[1],
                    tableData.getQueryJob().getId(),
                    request,
                    parseCredentialsHeader(clientSuppliedCredentials));
            }
        } catch (Exception ex) {
            throw new TableApiErrorException(ex, TableData::errorInstance);
        }

        return tableData;
    }

    @AuditActionUri("data-connect:next-page")
    @AuditIgnoreHeaders("GA4GH-Search-Authorization")
    @AuditEventCustomize(QueryJobAppenderAuditEventCustomizer.class)
    @PreAuthorize("hasAuthority('SCOPE_data-connect:query') && hasAuthority('SCOPE_data-connect:data') && @accessEvaluator.canAccessResource('/search/', {'data-connect:query', 'data-connect:data'}, {'data-connect:query', 'data-connect:data'})")
    @GetMapping(value = "/search/**")
    public TableData getNextPaginatedResponse(@RequestParam("queryJobId") String queryJobId,
                                              HttpServletRequest request,
                                              @AuditIgnore @RequestHeader(value = "GA4GH-Search-Authorization", defaultValue = "") List<String> clientSuppliedCredentials) {
        String page = request.getRequestURI()
                             .split(request.getContextPath() + "/search/")[1];
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
    @DeleteMapping(value = "/search/**")
    public ResponseEntity<?> deleteSearchQuery(@RequestParam("queryJobId") String queryJobId) {
        log.info("Terminating query with ID: {}", queryJobId);
        trinoDataConnectAdapter.deleteQueryJob(queryJobId);
        return ResponseEntity.noContent().build();
    }

    // TODO make this method into a Spring MVC parameter provider
    public static Map<String, String> parseCredentialsHeader(List<String> clientSuppliedCredentials) {
        return clientSuppliedCredentials.stream()
            .map(val -> val.split("=", 2))
            .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));
    }

}
