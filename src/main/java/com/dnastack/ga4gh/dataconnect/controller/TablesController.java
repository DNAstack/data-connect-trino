package com.dnastack.ga4gh.dataconnect.controller;

import com.dnastack.audit.aspect.AuditActionUri;
import com.dnastack.audit.aspect.AuditEventCustomize;
import com.dnastack.audit.aspect.AuditIgnore;
import com.dnastack.audit.aspect.AuditIgnoreHeaders;
import com.dnastack.ga4gh.dataconnect.adapter.shared.QueryJobAppenderAuditEventCustomizer;
import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoDataConnectAdapter;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.TableApiErrorException;
import com.dnastack.ga4gh.dataconnect.model.TableData;
import com.dnastack.ga4gh.dataconnect.model.TableInfo;
import com.dnastack.ga4gh.dataconnect.model.TablesList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@RestController
public class TablesController {

    private final TrinoDataConnectAdapter trinoDataConnectAdapter;

    @Autowired
    public TablesController(TrinoDataConnectAdapter trinoDataConnectAdapter) {
        this.trinoDataConnectAdapter = trinoDataConnectAdapter;
    }

    @AuditActionUri("data-connect:info")
    @AuditIgnoreHeaders("GA4GH-Search-Authorization")
    @PreAuthorize("hasAuthority('SCOPE_data-connect:info') && @accessEvaluator.canAccessResource('/tables', 'data-connect:info', 'data-connect:info')")
    @GetMapping(value = "/tables")
    public ResponseEntity<TablesList> getTables(HttpServletRequest request,
                                                @AuditIgnore @RequestHeader(value = "GA4GH-Search-Authorization", defaultValue = "") List<String> clientSuppliedCredentials) {
        TablesList tablesList;

        try {
            tablesList = trinoDataConnectAdapter
                .getTables(request, DataConnectController.parseCredentialsHeader(clientSuppliedCredentials));
        } catch (Exception ex) {
            throw new TableApiErrorException(ex, TablesList::errorInstance);
        }

        return ResponseEntity.ok().headers(getExtraAuthHeaders(tablesList)).body(tablesList);
    }

    // This endpoint is in addition to GET /tables to allow random-access to pages in the GET /tables result
    @AuditActionUri("data-connect:get-tables-in-catalog")
    @AuditIgnoreHeaders("GA4GH-Search-Authorization")
    @PreAuthorize("hasAuthority('SCOPE_data-connect:info') && @accessEvaluator.canAccessResource('/tables/catalog/' + #catalogName, 'data-connect:info', 'data-connect:info')")
    @GetMapping(value = "/tables/catalog/{catalogName}")
    public ResponseEntity<TablesList> getTablesByCatalog(@PathVariable("catalogName") String catalogName,
                                                         HttpServletRequest request,
                                                         @AuditIgnore @RequestHeader(value = "GA4GH-Search-Authorization", defaultValue = "") List<String> clientSuppliedCredentials) {
        TablesList tablesList;

        try {
            tablesList = trinoDataConnectAdapter
                .getTablesInCatalog(catalogName, request, DataConnectController.parseCredentialsHeader(clientSuppliedCredentials));
        } catch (Exception ex) {
            throw new TableApiErrorException(ex, TablesList::errorInstance);
        }

        return ResponseEntity.ok().headers(getExtraAuthHeaders(tablesList)).body(tablesList);

    }

    @AuditActionUri("data-connect:get-table-info")
    @AuditIgnoreHeaders("GA4GH-Search-Authorization")
    @PreAuthorize("hasAuthority('SCOPE_data-connect:info') && @accessEvaluator.canAccessResource('/table/' + #table_name + '/info', 'data-connect:info', 'data-connect:info')")
    @GetMapping(value = "/table/{table_name}/info")
    public TableInfo getTableInfo(@PathVariable("table_name") String tableName,
                                  HttpServletRequest request,
                                  @AuditIgnore @RequestHeader(value = "GA4GH-Search-Authorization", defaultValue = "") List<String> clientSuppliedCredentials) {

        TableInfo tableInfo;

        try {
            log.debug("Getting info for table {}", tableName);
            tableInfo = trinoDataConnectAdapter
                .getTableInfo(tableName, request, DataConnectController.parseCredentialsHeader(clientSuppliedCredentials));
        } catch (Exception ex) {
            throw new TableApiErrorException(ex, TableInfo::errorInstance);
        }

        return tableInfo;
    }

    @AuditActionUri("data-connect:get-table-data")
    @AuditIgnoreHeaders("GA4GH-Search-Authorization")
    @AuditEventCustomize(QueryJobAppenderAuditEventCustomizer.class)
    @PreAuthorize("hasAuthority('SCOPE_data-connect:data') && @accessEvaluator.canAccessResource('/table/' + #table_name + '/data', 'data-connect:data', 'data-connect:data')")
    @GetMapping(value = "/table/{table_name}/data")
    public TableData getTableData(@PathVariable("table_name") String tableName,
                                  HttpServletRequest request,
                                  @AuditIgnore @RequestHeader(value = "GA4GH-Search-Authorization", defaultValue = "") List<String> clientSuppliedCredentials) {

        TableData tableData;

        try {
            tableData = trinoDataConnectAdapter
                .getTableData(tableName, request, DataConnectController.parseCredentialsHeader(clientSuppliedCredentials));
        } catch (Exception ex) {
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
