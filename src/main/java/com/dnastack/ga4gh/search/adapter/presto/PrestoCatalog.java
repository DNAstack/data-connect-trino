package com.dnastack.ga4gh.search.adapter.presto;

import com.dnastack.ga4gh.search.model.DataModel;
import com.dnastack.ga4gh.search.model.Pagination;
import com.dnastack.ga4gh.search.model.TableData;
import com.dnastack.ga4gh.search.model.TableInfo;
import com.dnastack.ga4gh.search.model.TablesList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class PrestoCatalog {
    private final PrestoSearchAdapter searchAdapter;
    private final ThrowableTransformer throwableTransformer;

    /**
     * This is a URL scheme + host + port + path prefix. Never ends with "/".
     */
    private final String callbackBaseUrl;

    private final String catalogName;

    private static final String QUERY_TABLE_TEMPLATE =
            "SELECT table_catalog, table_schema, table_name" +
            " FROM %s.information_schema.tables" +
            " WHERE table_schema != 'information_schema'" +
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

    private List<TableInfo> combineTableInfo(List<List<TableInfo>> tableInfoLists) {
        return tableInfoLists.stream()
                             .flatMap(innerList->innerList.stream())
                             .collect(Collectors.toList());
    }

    private static String quote(String sqlIdentifier) {
        return "\"" + sqlIdentifier.replace("\"", "\"\"") + "\"";
    }

    public TablesList getTablesList(Pagination nextPage, HttpServletRequest request, Map<String, String> extraCredentials) {
        try {
            TableData tables = searchAdapter.searchAll(String.format(QUERY_TABLE_TEMPLATE, quote(catalogName)), request, extraCredentials, null);
            List<TableInfo> tableInfoList = getTableInfoList(tables);
            return new TablesList(tableInfoList, null, nextPage);
        } catch (Throwable t) {
            if (log.isTraceEnabled()) {
                log.error("Error when fetching tables for {}", catalogName, t);
            }
            return new TablesList(null, throwableTransformer.transform(t, catalogName), null);
        }
    }
}
