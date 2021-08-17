package com.dnastack.ga4gh.dataconnect.controller;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoDataConnectAdapter;
import com.dnastack.ga4gh.dataconnect.adapter.trino.DataConnectRequest;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.TableApiErrorException;
import com.dnastack.ga4gh.dataconnect.model.TableData;
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
public class DataConnectController {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TrinoDataConnectAdapter trinoDataConnectAdapter;

    @PreAuthorize("hasAuthority('SCOPE_search:query') && hasAuthority('SCOPE_search:data') && @accessEvaluator.canAccessResource('/search', {'search:query', 'search:data'}, {'search:query', 'search:data'})")
    @RequestMapping(value = "/search", method = RequestMethod.POST)
    public TableData search(@RequestBody DataConnectRequest dataConnectRequest,
                            HttpServletRequest request,
                            @RequestHeader(value = "GA4GH-Search-Authorization", defaultValue = "") List<String> clientSuppliedCredentials) {
        TableData tableData = null;

        try {
            log.info("/search query= {}", dataConnectRequest.getSqlQuery());
            tableData = trinoDataConnectAdapter
                .search(dataConnectRequest.getSqlQuery(), request, parseCredentialsHeader(clientSuppliedCredentials), null);
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
        log.info("/search/** request= {}", request);
        String page = request.getRequestURI()
                             .split(request.getContextPath() + "/search/")[1];
        log.info("/search/** page= {}", page);
        TableData tableData = null;

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

    // TODO make this method into a Spring MVC parameter provider
    public static Map<String, String> parseCredentialsHeader(List<String> clientSuppliedCredentials) {
        return clientSuppliedCredentials.stream()
            .map(val -> val.split("=", 2))
            .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));
    }

}
