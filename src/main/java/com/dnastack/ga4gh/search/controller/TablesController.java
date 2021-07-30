package com.dnastack.ga4gh.search.controller;

import com.dnastack.ga4gh.search.adapter.presto.PrestoSearchAdapter;
import com.dnastack.ga4gh.search.adapter.presto.exception.TableApiErrorException;
import com.dnastack.ga4gh.search.model.TableData;

import java.util.LinkedList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import com.dnastack.ga4gh.search.model.TableInfo;
import com.dnastack.ga4gh.search.model.TablesList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TablesController {

    @Autowired
    private PrestoSearchAdapter prestoSearchAdapter;

    @PreAuthorize("hasAuthority('SCOPE_search:info') && @accessEvaluator.canAccessResource('/tables', 'search:info', 'search:info')")
    @RequestMapping(value = "/tables", method = RequestMethod.GET)
    public ResponseEntity<TablesList> getTables(HttpServletRequest request,
                                                @RequestHeader(value = "GA4GH-Search-Authorization", defaultValue = "") List<String> clientSuppliedCredentials) {
        TablesList tablesList = null;

        try {
            tablesList = prestoSearchAdapter
                .getTables(request, SearchController.parseCredentialsHeader(clientSuppliedCredentials));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TableApiErrorException(ex, TablesList::errorInstance);
        }

        return ResponseEntity.ok().headers(getExtraAuthHeaders(tablesList)).body(tablesList);
    }

    // This endpoint is in addition to GET /tables to allow random-access to pages in the GET /tables result
    @PreAuthorize("hasAuthority('SCOPE_search:info') && @accessEvaluator.canAccessResource('/tables/catalog/' + #catalogName, 'search:info', 'search:info')")
    @RequestMapping(value = "/tables/catalog/{catalogName}", method = RequestMethod.GET)
    public ResponseEntity<TablesList> getTablesByCatalog(@PathVariable("catalogName") String catalogName,
                                                         HttpServletRequest request,
                                                         @RequestHeader(value = "GA4GH-Search-Authorization", defaultValue = "") List<String> clientSuppliedCredentials) {
        TablesList tablesList = null;

        try {
            tablesList = prestoSearchAdapter
                .getTablesInCatalog(catalogName, request, SearchController.parseCredentialsHeader(clientSuppliedCredentials));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TableApiErrorException(ex, TablesList::errorInstance);
        }

        return ResponseEntity.ok().headers(getExtraAuthHeaders(tablesList)).body(tablesList);

    }

    @PreAuthorize("hasAuthority('SCOPE_search:info') && @accessEvaluator.canAccessResource('/table/' + #table_name + '/info', 'search:info', 'search:info')")
    @RequestMapping(value = "/table/{table_name}/info", method = RequestMethod.GET)
    public TableInfo getTableInfo(@PathVariable("table_name") String tableName,
                                  HttpServletRequest request,
                                  @RequestHeader(value = "GA4GH-Search-Authorization", defaultValue = "") List<String> clientSuppliedCredentials) {

        TableInfo tableInfo = null;

        try {
            tableInfo = prestoSearchAdapter
                .getTableInfo(tableName, request, SearchController.parseCredentialsHeader(clientSuppliedCredentials));
        } catch (Exception ex) {
            throw new TableApiErrorException(ex, TableInfo::errorInstance);
        }

        return tableInfo;
    }

    @PreAuthorize("hasAuthority('SCOPE_search:data') && @accessEvaluator.canAccessResource('/table/' + #table_name + '/data', 'search:data', 'search:data')")
    @RequestMapping(value = "/table/{table_name}/data", method = RequestMethod.GET)
    public TableData getTableData(@PathVariable("table_name") String tableName,
                                  HttpServletRequest request,
                                  @RequestHeader(value = "GA4GH-Search-Authorization", defaultValue = "") List<String> clientSuppliedCredentials) {

        TableData tableData = null;

        try {
            tableData = prestoSearchAdapter
                .getTableData(tableName, request, SearchController.parseCredentialsHeader(clientSuppliedCredentials));
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new TableApiErrorException(ex, TableData::errorInstance);
        }

        return tableData;
    }

    private static String escapeQuotes(String s) {
        return s.replace("\"", "\\\"");
    }

    private HttpHeaders getExtraAuthHeaders(TablesList listTables) {
        HttpHeaders headers = new HttpHeaders();

        if (listTables.getErrors() != null) {
            final List<String> unauthenticatedRealmNames = new LinkedList<>();

            listTables.getErrors().forEach(error -> {
                if (error.getStatus() == HttpStatus.UNAUTHORIZED.value()) {
                    unauthenticatedRealmNames.add(error.getSource());
                }
            });

            if (listTables.getErrors().size() == unauthenticatedRealmNames.size()) {
                headers.add("WWW-Authenticate",
                    "GA4GH-Search realm:\"" + escapeQuotes(unauthenticatedRealmNames.get(0)) + "\"");
            }
        }

        return headers;
    }
}
