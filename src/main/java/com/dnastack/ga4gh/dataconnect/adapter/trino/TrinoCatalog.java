package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.dnastack.ga4gh.dataconnect.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
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

    private static final String QUERY_TABLE_TEMPLATE =
            "SELECT table_catalog, table_schema, table_name" +
            " FROM %s.information_schema.tables" +
            " WHERE table_schema != 'information_schema'" +
            " AND table_type IN ('BASE TABLE','VIEW')" +
            " ORDER BY 1, 2, 3";

    private TableInfo getTableInfo(Map<String, Object> row) {
        String schema = (String) row.get("table_schema");
        String table = (String) row.get("table_name");
        String qualifiedTableName = catalogName + "." + schema + "." + table;
        String ref = String.format("%s/table/%s/info", callbackBaseUrl, qualifiedTableName);
        log.trace("Got table "+qualifiedTableName);
        return new TableInfo(qualifiedTableName, null, DataModel.builder().ref(ref).build(), null);
    }

    private List<TableInfo> getTableInfoList(TableData tableData) {
        return tableData.getData().stream()
                        .map(this::getTableInfo)
                        .collect(Collectors.toList());
    }

    private static String quote(String sqlIdentifier) {
        return "\"" + sqlIdentifier.replace("\"", "\"\"") + "\"";
    }

    public TablesList getTablesList(Pagination nextPage, HttpServletRequest request, Map<String, String> extraCredentials) {
        try {
            TableData tables = dataConnectAdapter.searchAll(String.format(QUERY_TABLE_TEMPLATE, quote(catalogName)), request, extraCredentials, null);
            List<TableInfo> tableInfoList = getTableInfoList(tables);
            return new TablesList(tableInfoList, null, nextPage);
        } catch (Throwable t) {
            TableError tableError = TableError.fromThrowable(t, catalogName);
            log.info("Including error in response body: {}", tableError);
            return new TablesList(null, tableError, nextPage);
        }
    }
}
