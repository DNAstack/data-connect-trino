package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.dnastack.ga4gh.dataconnect.model.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class TrinoCatalogTest {
    private static final String CALLBACK_BASE_URL = "http://localhost";
    private static final String CATALOG_NAME = "testCatalog";
    private static final String SCHEMA_NAME = "testSchema";
    private static final String TABLE_NAME_1 = "testTable1";
    private static final String TABLE_NAME_2 = "testTable2";

    private static String quote(String sqlIdentifier) {
        return "\"" + sqlIdentifier.replace("\"", "\"\"") + "\"";
    }

    private static final String EXPECTED_SQL_WITH_SCHEMA = String.format(
            """
                    SELECT table_catalog, table_schema, table_name
                     FROM %s.information_schema.tables
                     WHERE table_schema != 'information_schema'
                     AND table_schema = '%s'
                     AND table_type IN ('BASE TABLE','VIEW')
                    UNION
                    SELECT table_catalog, table_schema, table_name
                     FROM %s.information_schema.views
                     WHERE table_schema != 'information_schema'
                     AND table_schema = '%s'
                    ORDER BY 1, 2, 3
                    """,
            quote(CATALOG_NAME), quote(SCHEMA_NAME), quote(CATALOG_NAME), quote(SCHEMA_NAME)
    );

    private static final String EXPECTED_SQL_WITHOUT_SCHEMA = String.format(
            """
                    SELECT table_catalog, table_schema, table_name
                     FROM %s.information_schema.tables
                     WHERE table_schema != 'information_schema'
                     AND table_type IN ('BASE TABLE','VIEW')
                    UNION
                    SELECT table_catalog, table_schema, table_name
                     FROM %s.information_schema.views
                     WHERE table_schema != 'information_schema'
                    ORDER BY 1, 2, 3
                    """,
            quote(CATALOG_NAME), quote(CATALOG_NAME)
    );

    @Mock
    private TrinoDataConnectAdapter dataConnectAdapter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private TableData tableDataMock;

    @Captor
    private ArgumentCaptor<Map<String, String>> credentialsCaptor;

    private TrinoCatalog trinoCatalogWithSchema;
    private TrinoCatalog trinoCatalogWithoutSchema;
    private Pagination nextPage;
    private Map<String, String> testCredentials;

    @Before
    public void setUp() {
        nextPage = new Pagination(null, null, null);
        testCredentials = Collections.singletonMap("X-Custom-Auth", "secret-token");

        trinoCatalogWithSchema = new TrinoCatalog(dataConnectAdapter, CALLBACK_BASE_URL, CATALOG_NAME, SCHEMA_NAME);
        trinoCatalogWithoutSchema = new TrinoCatalog(dataConnectAdapter, CALLBACK_BASE_URL, CATALOG_NAME);

        when(tableDataMock.getData()).thenReturn(Collections.emptyList());
    }

    private Map<String, Object> createTableDataRow(String schema, String tableName) {
        Map<String, Object> row = new HashMap<>();
        row.put("table_catalog", CATALOG_NAME);
        row.put("table_schema", schema);
        row.put("table_name", tableName);
        return row;
    }

    @Test
    public void getTablesList_withSchema_shouldQueryCorrectSchemaAndFormatResults() {
        List<Map<String, Object>> dataList = Collections.singletonList(createTableDataRow(SCHEMA_NAME, TABLE_NAME_1));
        when(tableDataMock.getData()).thenReturn(dataList);
        when(dataConnectAdapter.searchAll(
                eq(EXPECTED_SQL_WITH_SCHEMA),
                eq(request),
                eq(Collections.emptyMap()),
                isNull()))
                .thenReturn(tableDataMock);

        TablesList result = trinoCatalogWithSchema.getTablesList(nextPage, request, Collections.emptyMap());

        assertNotNull(result);
        assertNull(result.getErrors());
        assertThat(result.getTableInfos(), hasSize(1));
        assertEquals(nextPage, result.getPagination());

        List<TableInfo> tables = result.getTableInfos();
        assertNotNull(tables);
        assertThat(tables, hasSize(1));

        TableInfo tableInfo = tables.getFirst();
        String expectedQualifiedName = CATALOG_NAME + "." + SCHEMA_NAME + "." + TABLE_NAME_1;
        assertEquals(expectedQualifiedName, tableInfo.getName());
        String expectedRef = CALLBACK_BASE_URL + "/table/" + expectedQualifiedName + "/info";
        assertEquals(expectedRef, tableInfo.getDataModel().getRef());

        verify(dataConnectAdapter).searchAll(
                eq(EXPECTED_SQL_WITH_SCHEMA),
                eq(request),
                eq(Collections.emptyMap()),
                isNull());
    }

    @Test
    public void getTablesList_withoutSchema_shouldQueryAllSchemasAndPassCredentials() {
        List<Map<String, Object>> dataList = Collections.singletonList(createTableDataRow("anotherSchema", TABLE_NAME_1));
        when(tableDataMock.getData()).thenReturn(dataList);
        when(dataConnectAdapter.searchAll(
                eq(EXPECTED_SQL_WITHOUT_SCHEMA),
                eq(request),
                eq(testCredentials),
                isNull()))
                .thenReturn(tableDataMock);

        TablesList result = trinoCatalogWithoutSchema.getTablesList(nextPage, request, testCredentials);

        assertNull(result.getErrors());
        assertThat(result.getTableInfos(), hasSize(1));

        assertEquals(nextPage, result.getPagination());

        List<TableInfo> tables = result.getTableInfos();
        assertThat(tables, hasSize(1));

        TableInfo tableInfo = tables.getFirst();
        String expectedQualifiedName = CATALOG_NAME + ".anotherSchema." + TABLE_NAME_1;
        assertEquals(expectedQualifiedName, tableInfo.getName());
        String expectedRef = CALLBACK_BASE_URL + "/table/" + expectedQualifiedName + "/info";
        assertEquals(expectedRef, tableInfo.getDataModel().getRef());

        verify(dataConnectAdapter).searchAll(
                eq(EXPECTED_SQL_WITHOUT_SCHEMA),
                eq(request),
                credentialsCaptor.capture(),
                isNull());
        assertEquals(testCredentials, credentialsCaptor.getValue());
    }

    @Test
    public void getTablesList_withMultipleTables_shouldReturnAll() {
        List<Map<String, Object>> dataList = Arrays.asList(
                createTableDataRow(SCHEMA_NAME, TABLE_NAME_1),
                createTableDataRow(SCHEMA_NAME, TABLE_NAME_2)
        );
        when(tableDataMock.getData()).thenReturn(dataList);
        when(dataConnectAdapter.searchAll(
                eq(EXPECTED_SQL_WITH_SCHEMA), any(), anyMap(), isNull()))
                .thenReturn(tableDataMock);

        TablesList result = trinoCatalogWithSchema.getTablesList(nextPage, request, Collections.emptyMap());

        assertNull(result.getErrors());

        List<TableInfo> tables = result.getTableInfos();
        assertThat(tables, hasSize(2));

        TableInfo tableInfo1 = tables.getFirst();
        String expectedQualifiedName1 = CATALOG_NAME + "." + SCHEMA_NAME + "." + TABLE_NAME_1;
        assertEquals(expectedQualifiedName1, tableInfo1.getName());
        String expectedRef1 = CALLBACK_BASE_URL + "/table/" + expectedQualifiedName1 + "/info";
        assertEquals(expectedRef1, tableInfo1.getDataModel().getRef());

        TableInfo tableInfo2 = tables.get(1);
        String expectedQualifiedName2 = CATALOG_NAME + "." + SCHEMA_NAME + "." + TABLE_NAME_2;
        assertEquals(expectedQualifiedName2, tableInfo2.getName());
        String expectedRef2 = CALLBACK_BASE_URL + "/table/" + expectedQualifiedName2 + "/info";
        assertEquals(expectedRef2, tableInfo2.getDataModel().getRef());
    }

    @Test
    public void getTablesList_whenAdapterReturnsEmptyDataList_shouldReturnEmptyList() {
        when(tableDataMock.getData()).thenReturn(Collections.emptyList());
        when(dataConnectAdapter.searchAll(eq(EXPECTED_SQL_WITH_SCHEMA), any(), anyMap(), isNull()))
                .thenReturn(tableDataMock);

        TablesList result = trinoCatalogWithSchema.getTablesList(nextPage, request, Collections.emptyMap());

        assertNull(result.getErrors());

        assertEquals(nextPage, result.getPagination());
        List<TableInfo> tables = result.getTableInfos();
        assertNotNull(tables);
        assertThat(tables, is(empty()));
    }

    @Test
    public void getTablesList_whenAdapterReturnsTableDataWithNullDataList_shouldReturnError() {
        when(tableDataMock.getData()).thenReturn(null);
        when(dataConnectAdapter.searchAll(eq(EXPECTED_SQL_WITH_SCHEMA), any(), anyMap(), isNull()))
                .thenReturn(tableDataMock);

        TablesList result = trinoCatalogWithSchema.getTablesList(nextPage, request, Collections.emptyMap());

        assertNotNull(result);
        assertNull(result.getTableInfos());
        assertEquals(nextPage, result.getPagination());

        assertNotNull(result.getErrors());
        assertThat(result.getErrors(), hasSize(1));
        TableError error = result.getErrors().getFirst();
        assertThat(error.getTitle(), containsStringIgnoringCase("Encountered an unexpected error"));
        assertThat(error.getDetails(), containsString("NullPointerException"));
        assertEquals(500, error.getStatus());
    }

    @Test
    public void getTablesList_whenAdapterReturnsNullTableData_shouldReturnError() {
        when(dataConnectAdapter.searchAll(eq(EXPECTED_SQL_WITH_SCHEMA), any(), anyMap(), isNull()))
                .thenReturn(null);

        TablesList result = trinoCatalogWithSchema.getTablesList(nextPage, request, Collections.emptyMap());

        assertNotNull(result);
        assertNull(result.getTableInfos());
        assertEquals(nextPage, result.getPagination());

        assertNotNull(result.getErrors());
        assertThat(result.getErrors(), hasSize(1));
        TableError error = result.getErrors().getFirst();
        assertThat(error.getTitle(), containsStringIgnoringCase("Encountered an unexpected error"));
        assertThat(error.getDetails(), containsString("NullPointerException"));
        assertEquals(500, error.getStatus());
    }

    @Test
    public void getTablesList_whenAdapterThrowsException_shouldReturnTablesListWithError() {
        String exceptionMessage = "Connection refused";
        RuntimeException testException = new RuntimeException(exceptionMessage);
        when(dataConnectAdapter.searchAll(eq(EXPECTED_SQL_WITH_SCHEMA), eq(request), eq(Collections.emptyMap()), isNull()))
                .thenThrow(testException);

        TablesList result = trinoCatalogWithSchema.getTablesList(nextPage, request, Collections.emptyMap());

        assertNotNull(result);
        assertNull(result.getTableInfos());
        assertEquals(nextPage, result.getPagination());

        assertNotNull(result.getErrors());
        assertThat(result.getErrors(), hasSize(1));
        TableError error = result.getErrors().getFirst();
        assertThat(error.getTitle(), containsStringIgnoringCase("Encountered an unexpected error"));
        assertThat(error.getDetails(), containsString("java.lang.RuntimeException"));
        assertEquals(500, error.getStatus());
    }
}