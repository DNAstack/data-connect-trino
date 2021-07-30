package com.dnastack.ga4gh.search.controller;

import com.dnastack.ga4gh.search.adapter.presto.PrestoSearchAdapter;
import com.dnastack.ga4gh.search.adapter.presto.SearchRequest;
import com.dnastack.ga4gh.search.adapter.presto.exception.TableApiErrorException;
import com.dnastack.ga4gh.search.model.TableData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class SearchController {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private PrestoSearchAdapter prestoSearchAdapter;

    @PreAuthorize("hasAuthority('SCOPE_search:query') && hasAuthority('SCOPE_search:data') && @accessEvaluator.canAccessResource('/search', {'search:query', 'search:data'}, {'search:query', 'search:data'})")
    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public TableData search(@RequestBody SearchRequest searchRequest,
                            HttpServletRequest request,
                            @RequestHeader(value = "GA4GH-Search-Authorization", defaultValue = "") List<String> clientSuppliedCredentials) {
        TableData tableData = null;

        try {
            tableData = prestoSearchAdapter
                .search(searchRequest.getSqlQuery(), request, parseCredentialsHeader(clientSuppliedCredentials), null);
        } catch (Exception ex) {
            throw new TableApiErrorException(ex, TableData::errorInstance);
        }

        return tableData;
    }

    @PreAuthorize("hasAuthority('SCOPE_search:query') && hasAuthority('SCOPE_search:data') && @accessEvaluator.canAccessResource('/search/', {'search:query', 'search:data'}, {'search:query', 'search:data'})")
    @RequestMapping(value = "/search/**", method = RequestMethod.GET)
    public TableData getNextPaginatedResponse(@RequestParam("queryJobId") String queryJobId,
                                              HttpServletRequest request,
                                              @RequestHeader(value = "GA4GH-Search-Authorization", defaultValue = "") List<String> clientSuppliedCredentials) {
        String page = request.getRequestURI()
                             .split(request.getContextPath() + "/search/")[1];
        TableData tableData = null;

        try {
            tableData = prestoSearchAdapter
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
            String prestoNextURL = "NULL";
            if (tableData.getPagination() != null) {
                nextURL = (tableData.getPagination().getNextPageUrl() == null)
                          ? "null"
                          : tableData.getPagination().getNextPageUrl().toString();
                prestoNextURL = (tableData.getPagination().getPrestoNextPageUrl() == null)
                                ? "null"
                                : tableData.getPagination().getPrestoNextPageUrl().toString();
            }

            if(log.isTraceEnabled()) {
                try {

                    String json = objectMapper.writeValueAsString(tableData);
                    log.debug("Returning " + tableDataLength + " rows with nextURL=" + nextURL + " and prestoNextURL=" + prestoNextURL + " json: " + json);
                } catch (JsonProcessingException e) {
                    log.error("Error producing debug log output ", e);
                }
            }else{
                log.debug("Returning " + tableDataLength + " rows with nextURL=" + nextURL + " and prestoNextURL=" + prestoNextURL);
            }
        }
        return tableData;
    }

    // TODO make this method into a Spring MVC parameter provider
    public static Map<String, String> parseCredentialsHeader(List<String> clientSuppliedCredentials) {
        return clientSuppliedCredentials.stream()
            .map(val -> val.split("=", 2))
            .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));
    }

}
