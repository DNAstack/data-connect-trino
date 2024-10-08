package com.dnastack.ga4gh.dataconnect.adapter.trino;

import brave.Tracer;
import brave.Tracing;
import com.dnastack.ga4gh.dataconnect.DataModelSupplier;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.TrinoUserUnauthorizedException;
import com.dnastack.ga4gh.dataconnect.model.DataModel;
import com.dnastack.ga4gh.dataconnect.model.TableData;
import com.dnastack.ga4gh.dataconnect.repository.QueryJob;
import com.dnastack.ga4gh.dataconnect.repository.QueryJobDao;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.extension.ExtensionCallback;
import org.jdbi.v3.core.extension.ExtensionConsumer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.UncheckedIOException;
import java.net.URI;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
public class TrinoDataConnectAdapterTest {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private Tracing tracing;
    private Tracer.SpanInScope spanInScope;

    /** The object under test */
    TrinoDataConnectAdapter dataConnectAdapter;

    /**
     * The data model that will always be returned by the DataModelSupplier. Tests can modify this before calling
     * into dataConnectAdapter. */
    DataModel fakeDataModel;

    /**
     * The source of Trino responses. Tests should load it with Trino response data using
     * mockTrinoClient.setResponsePages().
     */
    MockTrinoClient mockTrinoClient = new MockTrinoClient();

    /**
     * Managed by the mock QueryJobDao:
     * Set when create(any) is called; returned when get(any) is called.
     */
    QueryJob currentQueryJob;

    static class MockTrinoClient implements TrinoClient {

        private Iterator<String> responsePageIterator;

        void setResponsePages(List<String> responsePages) {
            responsePageIterator = responsePages.iterator();
        }

        @Override
        public TrinoDataPage query(String statement, Map<String, String> extraCredentials) {
            return next("first", extraCredentials);
        }

        @Override
        public TrinoDataPage next(String page, Map<String, String> extraCredentials) {
            if (responsePageIterator == null) {
                throw new IllegalStateException("You need to set up some Trino responses by calling setResponsePages()");
            }
            try {
                return objectMapper.readValue(responsePageIterator.next(), TrinoDataPage.class);
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void killQuery(String nextPageUrl) {
            log.info("Something called MockTrinoClient.killQuery({})", nextPageUrl);
        }
    }
    @Before
    public void setUp() throws Exception {
        tracing = Tracing.newBuilder().build();
        spanInScope = tracing.tracer().withSpanInScope(tracing.tracer().nextSpan());

        // mock QueryJobDao and JDBI
        QueryJobDao queryJobDao = mock(QueryJobDao.class);
        doAnswer(invocation -> {
            currentQueryJob = invocation.getArgument(0);
            return currentQueryJob;
        }).when(queryJobDao).create(any(QueryJob.class));
        when(queryJobDao.get(any())).thenAnswer(invocation -> Optional.ofNullable(currentQueryJob));

        Jdbi jdbi = mock(Jdbi.class);
        doAnswer(invocation -> {
            ExtensionConsumer extensionConsumer = invocation.getArgument(1);
            extensionConsumer.useExtension(queryJobDao);
            return null;
        }).when(jdbi).useExtension(eq(QueryJobDao.class), any());
        doAnswer(invocation -> {
            ExtensionCallback extensionCallback = invocation.getArgument(1);
            return extensionCallback.withExtension(queryJobDao);
        }).when(jdbi).withExtension(eq(QueryJobDao.class), any());

        fakeDataModel = new DataModel(
                URI.create("https://exaple.com/test-data-model"),
                "Test data model",
                null,
                Map.of(),
                null);
        DataModelSupplier dataModelSupplier = tableName -> fakeDataModel;

        dataConnectAdapter = new TrinoDataConnectAdapter(
                mockTrinoClient, jdbi, null, List.of(dataModelSupplier), tracing
        );
    }

    @After
    public void tearDown() throws Exception {
        spanInScope.close();
        tracing.close();
    }

    @Test
    public void biFunctionPatternTest() {
        String jsonFunctionQuery = "select id, phenopacket from sample_phenopackets.ga4gh_tables.gecco_phenopackets " +
                "where json_extract_scalar(pp.phenopacket, '$.subject.sex') = 'MALE' limit 3";
        assertFalse(TrinoDataConnectAdapter.biFunctionPattern.matcher(jsonFunctionQuery).find());

        String ga4ghTypeFunctionQuery = "SELECT ga4gh_type(bogusfield, '$ref:http://path/to/whatever.com') FROM tableX";
        assertTrue(TrinoDataConnectAdapter.biFunctionPattern.matcher(ga4ghTypeFunctionQuery).find());
    }

    @Test
    public void getTableData_should_handleArrayWithNullEntries() throws Exception {
        mockTrinoClient.setResponsePages(List.of(
            //language=json
            """
            {
                "id": "fake-req-1",
                "nextUri": "http://example.com/fake-req-2"
            }
            """,
            //language=json
            """
            {
                "id": "fake-req-1",
                "columns": [
                    {
                        "name": "col1",
                        "typeSignature": {
                            "arguments": [
                                {
                                    "value": {
                                        "rawType": "string"
                                    }
                                }
                            ],
                            "rawType": "array"
                        }
                    }
                ],
                "data": [
                    [[ "a", "b", null, "a", null ]]
                ]
            }
            """
        ));

        // When I try to get table data
        dataConnectAdapter.getTableData("collections.c1.t1", new MockHttpServletRequest(), Map.of());
        TableData tableData = dataConnectAdapter.getNextSearchPage("", "fake-req-1", new MockHttpServletRequest(), Map.of());

        // Then
        assertThat("Ensure that the field is not empty",
            (Collection<?>) tableData.getData().get(0).get("col1"), not(empty()));
        assertThat("Ensure that null is included",
            (Collection<?>) tableData.getData().get(0).get("col1"), containsInRelativeOrder("a", "b", null));
    }

    @Test
    public void getTableData_should_handleNullArray() throws Exception {
        mockTrinoClient.setResponsePages(List.of(
            //language=json
            """
            {
                "id": "fake-req-1",
                "nextUri": "http://example.com/fake-req-2"
            }
            """,
            //language=json
            """
            {
                "id": "fake-req-1",
                "columns": [
                    {
                        "name": "col1",
                        "typeSignature": {
                            "arguments": [
                                {
                                    "value": {
                                        "rawType": "string"
                                    }
                                }
                            ],
                            "rawType": "array"
                        }
                    }
                ],
                "data": [
                    [null]
                ]
            }
            """
        ));

        // When I try to get table data
        dataConnectAdapter.getTableData("collections.c1.t1", new MockHttpServletRequest(), Map.of());
        TableData tableData = dataConnectAdapter.getNextSearchPage("", "fake-req-1", new MockHttpServletRequest(), Map.of());

        // Then
        assertThat("Ensure that the field is null",
            tableData.getData().get(0).get("col1"), Matchers.nullValue());
    }


    @Test
    public void getTableData_should_includeDataModel_when_supplierProvidesOne_and_tableIsEmpty() throws Exception {
        mockTrinoClient.setResponsePages(List.of(
            //language=json
            """
            {
                "id": "fake-req-1",
                "nextUri": "http://example.com/fake-req-2"
            }
            """,
            //language=json
            """
            {
                "id": "fake-req-1",
                "columns": [
                    { "name": "col1", "typeSignature": { "rawType": "varchar" } }
                ],
                "data": []
            }
            """
        ));

        // When I try to get table data
        dataConnectAdapter.getTableData("collections.c1.t1", new MockHttpServletRequest(), Map.of());
        TableData tableData = dataConnectAdapter.getNextSearchPage("", "fake-req-1", new MockHttpServletRequest(), Map.of());

        // Then
        assertThat("Adapter should not have found a row of data (this tests that we've mocked Trino correctly)",
                tableData.getData(), empty());
        assertThat("Should get the data model produced by the supplier",
                tableData.getDataModel(), equalTo(fakeDataModel));
    }

    @Test
    public void getTableData_should_includeDataModel_when_supplierProvidesOne_and_tableIsNotEmpty() {
        mockTrinoClient.setResponsePages(List.of(
                //language=json
                """
                {
                    "id": "fake-req-1",
                    "nextUri": "http://example.com/fake-req-2"
                }
                """,
                //language=json
                """
                {
                    "id": "fake-req-1",
                    "columns": [
                        { "name": "col1", "typeSignature": { "rawType": "varchar" } }
                    ],
                    "data": [
                        [ "val1" ]
                    ]
                }
                """
        ));

        // When I try to get table data
        dataConnectAdapter.getTableData("collections.c1.t1", new MockHttpServletRequest(), Map.of());
        TableData tableData = dataConnectAdapter.getNextSearchPage("", "fake-req-1", new MockHttpServletRequest(), Map.of());

        // Then
        assertThat("Adapter should have found a row of data (this tests that we've mocked Trino correctly)",
                tableData.getData(), containsInAnyOrder(hasEntry("col1", "val1")));
        assertThat("Should get the data model produced by the supplier",
                tableData.getDataModel(), equalTo(fakeDataModel));
    }

    @Test
    public void getTableData_should_generateADataModel_when_supplierProvidesNone_and_tableIsNotEmpty() {
        mockTrinoClient.setResponsePages(List.of(
                //language=json
                """
                {
                    "id": "fake-req-1",
                    "nextUri": "http://example.com/fake-req-2"
                }
                """,
                //language=json
                """
                {
                    "id": "fake-req-1",
                    "columns": [
                        { "name": "col1", "typeSignature": { "rawType": "varchar" } }
                    ],
                    "data": [
                        [ "val1" ]
                    ]
                }
                """
        ));
        fakeDataModel = null;

        // When I try to get table data
        dataConnectAdapter.getTableData("collections.c1.t1", new MockHttpServletRequest(), Map.of());
        TableData tableData = dataConnectAdapter.getNextSearchPage("", "fake-req-1", new MockHttpServletRequest(), Map.of());

        // Then
        assertThat("Adapter should have found a row of data (this tests that we've mocked Trino correctly)",
                tableData.getData(), containsInAnyOrder(hasEntry("col1", "val1")));
        assertThat("Should get the auto-generated data model",
                tableData.getDataModel(), notNullValue());
        assertThat("Should get the auto-generated data model",
                tableData.getDataModel().getDescription(), equalTo("Automatically generated schema"));
    }

    @Test
    public void getTableData_shouldNot_generateADataModel_when_supplierProvidesNone_and_tableIsEmpty() {
        mockTrinoClient.setResponsePages(List.of(
                //language=json
                """
                {
                    "id": "fake-req-1",
                    "nextUri": "http://example.com/fake-req-2"
                }
                """,
                //language=json
                """
                {
                    "id": "fake-req-1",
                    "data": []
                }
                """
        ));
        fakeDataModel = null;

        // When I try to get table data
        dataConnectAdapter.getTableData("collections.c1.t1", new MockHttpServletRequest(), Map.of());
        TableData tableData = dataConnectAdapter.getNextSearchPage("", "fake-req-1", new MockHttpServletRequest(), Map.of());

        // Then
        assertThat("Adapter should not have found a row of data (this tests that we've mocked Trino correctly)",
                tableData.getData(), empty());
        assertThat("Should not get a data model because there was none supplied and Trino gave no column metadata",
                tableData.getDataModel(), nullValue());
    }

    @Test
    public void getTableData_should_generateADataModel_when_supplierProvidesNone_and_trinoResponseHasColumnInfo() {
        mockTrinoClient.setResponsePages(List.of(
                //language=json
                """
                {
                    "id": "fake-req-1",
                    "nextUri": "http://example.com/fake-req-2"
                }
                """,
                //language=json
                """
                {
                    "id": "fake-req-1",
                    "columns": [
                        { "name": "col1", "typeSignature": { "rawType": "varchar" } }
                    ],
                    "data": []
                }
                """
        ));
        fakeDataModel = null;

        // When I try to get table data
        dataConnectAdapter.getTableData("collections.c1.t1", new MockHttpServletRequest(), Map.of());
        TableData tableData = dataConnectAdapter.getNextSearchPage("", "fake-req-1", new MockHttpServletRequest(), Map.of());

        // Then
        assertThat("Adapter should not have found a row of data (this tests that we've mocked Trino correctly)",
                tableData.getData(), empty());
        assertThat("Should get a data model generated from the column info",
                tableData.getDataModel().getDescription(), equalTo("Automatically generated schema"));
    }

    @Test
    public void getTableData_should_returnHttp401_when_trinoClientFindsMagicStringInThrowable() {
        mockTrinoClient.setResponsePages(List.of(
                //language=json
                """
                {
                  "id" : "20241008_160203_00004_gexz2",
                  "infoUri" : "https://localhost:8091/ui/query.html?20241008_160203_00004_gexz2",
                  "nextUri" : null,
                  "error" : {
                    "failureInfo" : {
                      "type" : "io.trino.spi.security.AccessDeniedException",
                      "message" : "Access Denied: HTTP 401: The Token has expired on 2024-10-08T00:51:16Z.",
                      "errorInfo" : {
                        "code" : "4",
                        "name" : "PERMISSION_DENIED",
                        "type" : "USER_ERROR"
                      },
                      "cause" : null,
                      "suppressed" : [ ],
                      "stack" : [
                        "com.dnastack.trino.plugin.PublisherSystemAccessControlFactory.lambda$create$0(PublisherSystemAccessControlFactory.java:163)",
                        "jdk.proxy17/jdk.proxy17.$Proxy618.checkCanSelectFromColumns(Unknown Source)",
                        "io.trino.security.AccessControlManager.lambda$checkCanSelectFromColumns$97(AccessControlManager.java:1098)",
                        "io.trino.security.AccessControlManager.systemAuthorizationCheck(AccessControlManager.java:1490)",
                        "io.trino.security.AccessControlManager.checkCanSelectFromColumns(AccessControlManager.java:1098)",
                        "io.trino.security.ForwardingAccessControl.checkCanSelectFromColumns(ForwardingAccessControl.java:422)",
                        "io.trino.tracing.TracingAccessControl.checkCanSelectFromColumns(TracingAccessControl.java:610)",
                        "io.trino.sql.analyzer.Analyzer.lambda$analyze$0(Analyzer.java:104)",
                        "java.base/java.util.LinkedHashMap.forEach(LinkedHashMap.java:986)",
                        "io.trino.sql.analyzer.Analyzer.lambda$analyze$1(Analyzer.java:103)",
                        "java.base/java.util.LinkedHashMap.forEach(LinkedHashMap.java:986)",
                        "io.trino.sql.analyzer.Analyzer.analyze(Analyzer.java:102)",
                        "io.trino.sql.analyzer.Analyzer.analyze(Analyzer.java:86)",
                        "io.trino.execution.SqlQueryExecution.analyze(SqlQueryExecution.java:285)",
                        "io.trino.execution.SqlQueryExecution.<init>(SqlQueryExecution.java:218)",
                        "io.trino.execution.SqlQueryExecution$SqlQueryExecutionFactory.createQueryExecution(SqlQueryExecution.java:892)",
                        "io.trino.dispatcher.LocalDispatchQueryFactory.lambda$createDispatchQuery$0(LocalDispatchQueryFactory.java:153)",
                        "io.trino.$gen.Trino_445____20241008_160046_2.call(Unknown Source)",
                        "com.google.common.util.concurrent.TrustedListenableFutureTask$TrustedFutureInterruptibleTask.runInterruptibly(TrustedListenableFutureTask.java:131)",
                        "com.google.common.util.concurrent.InterruptibleTask.run(InterruptibleTask.java:76)",
                        "com.google.common.util.concurrent.TrustedListenableFutureTask.run(TrustedListenableFutureTask.java:82)",
                        "java.base/java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1144)",
                        "java.base/java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:642)",
                        "java.base/java.lang.Thread.run(Thread.java:1570)"
                      ]
                    },
                    "message" : "Access Denied: HTTP 401: The Token has expired on 2024-10-08T00:51:16Z.",
                    "errorCode" : 4,
                    "errorName" : "PERMISSION_DENIED",
                    "errorType" : "USER_ERROR"
                  },
                  "columns" : null,
                  "data" : null,
                  "stats" : {
                    "state" : "FAILED"
                  }
                }
                """
        ));
        fakeDataModel = null;

        // When I try to get table data
        try {
            dataConnectAdapter.getTableData("collections.c1.t1", new MockHttpServletRequest(), Map.of());
            fail("Should have thrown a TrinoUserUnauthorizedException");
        } catch (TrinoUserUnauthorizedException e) {

            // then the exception should propagate an HTTP 401 status
            assertThat(e.getHttpStatus(), equalTo(HttpStatus.UNAUTHORIZED));

            // and the exception should contain the text of the underlying Trino error message
            assertThat(e.getMessage(), containsString("The Token has expired on 2024-10-08T00:51:16Z."));
        }
    }
}
