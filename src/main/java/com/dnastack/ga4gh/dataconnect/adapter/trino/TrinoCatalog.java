package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.dnastack.ga4gh.dataconnect.model.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class TrinoCatalog {
    private final TrinoDataConnectAdapter dataConnectAdapter;

    /**
     * This is a URL scheme + host + port + path prefix. Never ends with "/".
     */
    private final String callbackBaseUrl;
    private final String catalogName;
    private final String schemaName;

    public TrinoCatalog(TrinoDataConnectAdapter dataConnectAdapter, String callbackBaseUrl, String catalogName) {
        this(dataConnectAdapter, callbackBaseUrl, catalogName, null);
    }


    private TableInfo getTableInfo(Map<String, Object> row) {
        String schema = (String) row.get("table_schema");
        String table = (String) row.get("table_name");
        String qualifiedTableName = catalogName + "." + schema + "." + table;
        String ref = String.format("%s/table/%s/info", callbackBaseUrl, qualifiedTableName);
        log.trace("Got table " + qualifiedTableName);
        return new TableInfo(qualifiedTableName, null, DataModel.builder().ref(ref).build(), null);
    }

    private List<TableInfo> getTableInfoList(TableData tableData) {
        return tableData.getData().stream()
                .map(this::getTableInfo)
                .collect(Collectors.toList());
    }

    private static String quoteIdentifier(String sqlIdentifier) {
        return "\"" + sqlIdentifier.replace("\"", "\"\"") + "\"";
    }

    private static String quoteString(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    public TablesList getTablesList(Pagination nextPage, HttpServletRequest request, Map<String, String> extraCredentials) {
        try {
            String queryStatement;
            if (schemaName != null) {
                queryStatement = String.format("""
                        SELECT table_catalog, table_schema, table_name
                         FROM %s.information_schema.tables
                         WHERE table_schema != 'information_schema'
                         AND table_schema = %s
                         AND table_type IN ('BASE TABLE','VIEW')
                        UNION
                        SELECT table_catalog, table_schema, table_name
                         FROM %s.information_schema.views
                         WHERE table_schema != 'information_schema'
                         AND table_schema = %s
                        ORDER BY 1, 2, 3
                        """, quoteIdentifier(catalogName), quoteString(schemaName), quoteIdentifier(catalogName), quoteString(schemaName));
            } else {
                queryStatement = String.format("""
                        SELECT table_catalog, table_schema, table_name
                         FROM %s.information_schema.tables
                         WHERE table_schema != 'information_schema'
                         AND table_type IN ('BASE TABLE','VIEW')
                        UNION
                        SELECT table_catalog, table_schema, table_name
                         FROM %s.information_schema.views
                         WHERE table_schema != 'information_schema'
                        ORDER BY 1, 2, 3
                        """, quoteIdentifier(catalogName), quoteIdentifier(catalogName));
            }

            TableData tables = dataConnectAdapter.searchAll(queryStatement, request, extraCredentials, null);
            List<TableInfo> tableInfoList = getTableInfoList(tables);
            return new TablesList(tableInfoList, null, nextPage);
        } catch (Throwable t) {
            TableError tableError = TableError.fromThrowable(t, catalogName);
            log.info("Including error in response body: {}", tableError);
            return new TablesList(null, tableError, nextPage);
        }
    }
}