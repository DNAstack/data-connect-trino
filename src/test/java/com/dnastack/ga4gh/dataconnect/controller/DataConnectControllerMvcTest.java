package com.dnastack.ga4gh.dataconnect.controller;

import com.dnastack.ga4gh.dataconnect.DataConnectTrinoApplication;
import com.dnastack.ga4gh.dataconnect.adapter.security.ClientSuppliedCredentialsReader;
import com.dnastack.ga4gh.dataconnect.adapter.trino.DataConnectRequest;
import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoDataConnectAdapter;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.InvalidQueryJobException;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.TrinoNoSuchCatalogException;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.TrinoUnexpectedHttpResponseException;
import com.dnastack.ga4gh.dataconnect.model.*;
import com.dnastack.ga4gh.dataconnect.tenancy.TenantMirrorResolver;
import com.dnastack.ga4gh.dataconnect.repository.QueryJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.hamcrest.Matchers;
import org.jdbi.v3.core.Jdbi;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureEmbeddedDatabase(provider = ZONKY, refresh = AFTER_EACH_TEST_METHOD, type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        classes = DataConnectTrinoApplication.class,
        properties = "management.tracing.enabled=false"
)
@AutoConfigureMockMvc
@ActiveProfiles("no-auth")
public class DataConnectControllerMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoSpyBean
    private ClientSuppliedCredentialsReader clientSuppliedCredentialsReader;

    @MockitoBean
    private TrinoDataConnectAdapter trinoDataConnectAdapter;

    @MockitoBean
    private Jdbi jdbi; // Mock the JDBI instance if used

    // The request boundary asks the resolver whether the request's tenant can be served, and the real one reads
    // the mocked Jdbi.
    @MockitoBean
    private TenantMirrorResolver tenantResolver;


    private TablesList sampleTablesList;
    private String sampleCatalog = "test_catalog";
    private String sampleSchema = "test_schema";
    private String sampleTable = "table1";
    private String sampleQualifiedTableName = sampleCatalog + "." + sampleSchema + "." + sampleTable;
    private String sampleCredentialsHeader = "Bearer abc";
    private Map<String, String> expectedCredentialsMap = Map.of("Authorization", sampleCredentialsHeader);

    @Before
    public void setUp() {
        when(tenantResolver.isAccessible(any())).thenReturn(true);

        TableInfo tableInfo = new TableInfo(
                sampleQualifiedTableName,
                "Test table description",
                DataModel.builder().ref("http://example.com/ref/" + sampleQualifiedTableName).build(),
                null);
        Pagination pagination = new Pagination(null, URI.create("http://example.com/next"), null);
        sampleTablesList = new TablesList(List.of(tableInfo), null, pagination);
    }

    @Test
    public void getTables_should_returnOkAndTablesList() throws Exception {
        when(trinoDataConnectAdapter.getTables(any(), any()))
                .thenReturn(sampleTablesList);

        ResultActions resultActions = mockMvc.perform(get("/tables")
                .accept(MediaType.APPLICATION_JSON));

        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.tables", hasSize(1)))
                .andExpect(jsonPath("$.tables[0].name", equalTo(sampleQualifiedTableName)))
                .andExpect(jsonPath("$.pagination.next_page_url", equalTo("http://example.com/next")));

        verify(trinoDataConnectAdapter).getTables(any(), any());
    }

    @Test
    public void getTables_should_returnInternalServerError_when_theAdapterThrows() throws Exception {
        RuntimeException adapterException = new RuntimeException("Adapter failed");
        when(trinoDataConnectAdapter.getTables(any(), any()))
                .thenThrow(adapterException);

        ResultActions resultActions = mockMvc.perform(get("/tables")
                .accept(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isInternalServerError());

        verify(trinoDataConnectAdapter).getTables(any(), any());
    }

    @Test
    public void getTablesByCatalogAndSchema_should_returnOkAndTablesList() throws Exception {
        when(trinoDataConnectAdapter.getTablesByCatalogAndSchema(anyString(), anyString(), any(), any()))
                .thenReturn(sampleTablesList);

        ResultActions resultActions = mockMvc.perform(
                get("/tables/catalog/{catalogName}/schema/{schemaName}", sampleCatalog, sampleSchema)
                        .accept(MediaType.APPLICATION_JSON));
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(objectMapper.writeValueAsString(sampleTablesList)));

        verify(trinoDataConnectAdapter).getTablesByCatalogAndSchema(anyString(), anyString(), any(), any());
    }

    @Test
    public void getTablesByCatalogAndSchema_should_returnNotFound_when_theCatalogDoesNotExist() throws Exception {
        TrinoNoSuchCatalogException adapterException = new TrinoNoSuchCatalogException("Catalog not found: " + sampleCatalog);
        when(trinoDataConnectAdapter.getTablesByCatalogAndSchema(anyString(), anyString(), any(), any()))
                .thenThrow(adapterException);

        ResultActions resultActions = mockMvc.perform(
                get("/tables/catalog/{catalogName}/schema/{schemaName}", sampleCatalog, sampleSchema)
                        .accept(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isNotFound());

        verify(trinoDataConnectAdapter).getTablesByCatalogAndSchema(anyString(), anyString(), any(), any());
    }

    @Test
    public void getTablesByCatalogAndSchema_should_returnInternalServerError_when_theAdapterThrows() throws Exception {
        RuntimeException adapterException = new RuntimeException("Generic adapter failure");
        when(trinoDataConnectAdapter.getTablesByCatalogAndSchema(anyString(), anyString(), any(), any()))
                .thenThrow(adapterException);

        ResultActions resultActions = mockMvc.perform(
                get("/tables/catalog/{catalogName}/schema/{schemaName}", sampleCatalog, sampleSchema)
                        .accept(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isInternalServerError());

        verify(trinoDataConnectAdapter).getTablesByCatalogAndSchema(anyString(), anyString(), any(), any());
    }

    @Test
    public void deleteSearchQuery_should_relayThePageTheCallerOffered() throws Exception {
        String page = "v1/statement/executing/20260902_203359_48519_fnmag/y5bb5cace5500a2cf109b1c50c648b009c40a142f/4";
        String queryJobId = "20260902_203359_48519_fnmag";

        ResultActions resultActions = mockMvc.perform(
                delete("/search/" + page).param("queryJobId", queryJobId));

        resultActions.andExpect(status().isNoContent());

        verify(trinoDataConnectAdapter).deleteQueryJob(eq(page), eq(queryJobId), any());
    }

    @Test
    public void deleteSearchQuery_should_relayOnlyThePage_when_theRequestAddressesATenant() throws Exception {
        String page = "v1/statement/executing/20260902_203359_48519_fnmag/y5bb5cace5500a2cf109b1c50c648b009c40a142f/4";
        String queryJobId = "20260902_203359_48519_fnmag";
        String tenantId = "8e5f2a1c-0d3b-4e6a-9c7f-1b2d3e4f5a6b";

        ResultActions resultActions = mockMvc.perform(
                delete("/tenants/" + tenantId + "/search/" + page).param("queryJobId", queryJobId));

        resultActions.andExpect(status().isNoContent());

        // The tenant is already bound to the request by this point, and this service offers Trino the page alone.
        verify(trinoDataConnectAdapter).deleteQueryJob(eq(page), eq(queryJobId), any());
    }

    @Test
    public void search_should_readTheCredentialsHeaderOnce_when_itRetriesForData() throws Exception {
        // The header does not change between attempts, so the retries below must not read it again.
        DataConnectRequest request = new DataConnectRequest();
        request.setSqlQuery("SELECT * FROM test_table");
        TableData emptyPage = new TableData(
                DataModel.builder().ref("http://example.com/ref").build(),
                new ArrayList<>(),
                null,
                new Pagination(null, URI.create("http://localhost:8080/search/page2"), null),
                QueryJob.builder().id("test-job-123").build());
        when(trinoDataConnectAdapter.search(anyString(), any(), any(), any())).thenReturn(emptyPage);
        when(trinoDataConnectAdapter.getNextSearchPage(anyString(), anyString(), any(), any()))
                .thenReturn(emptyPage);

        mockMvc.perform(post("/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("GA4GH-Search-Authorization", "userToken=a.b.c")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // The retry exhausted its attempts, so the header was there to be re-read four times over.
        verify(trinoDataConnectAdapter, times(4)).getNextSearchPage(anyString(), anyString(), any(), any());
        verify(clientSuppliedCredentialsReader, times(1)).parse(List.of("userToken=a.b.c"));
    }

    /**
     * The three endpoint families return three different types on success, and one error body serves all three.
     * These assert that same body from each of them.
     */
    @Test
    public void anyEndpoint_should_answerTheSameShapedErrorBody() throws Exception {
        when(trinoDataConnectAdapter.getTables(any(), any()))
                .thenThrow(new InvalidQueryJobException("a-query-job"));
        when(trinoDataConnectAdapter.getTableInfo(anyString(), any(), any()))
                .thenThrow(new InvalidQueryJobException("a-query-job"));
        doThrow(new InvalidQueryJobException("a-query-job"))
                .when(trinoDataConnectAdapter).deleteQueryJob(anyString(), anyString(), any());

        Set<String> tablesShape = errorBodyShapeOf(get("/tables"));
        Set<String> tableInfoShape = errorBodyShapeOf(get("/table/{name}/info", "a_table"));
        Set<String> searchShape = errorBodyShapeOf(
                delete("/search/v1/statement/executing/a-query-job/slug/1").param("queryJobId", "a-query-job"));

        assertThat(tablesShape)
                .as("the error body of an endpoint returning TablesList, against one returning TableInfo")
                .isEqualTo(tableInfoShape);
        assertThat(searchShape)
                .as("the error body of an endpoint returning TableData, against one returning TableInfo")
                .isEqualTo(tableInfoShape);
    }

    @Test
    public void anyEndpoint_should_answerAnErrorBodyOfNothingButTheErrors() throws Exception {
        when(trinoDataConnectAdapter.getTables(any(), any()))
                .thenThrow(new InvalidQueryJobException("a-query-job"));

        mockMvc.perform(get("/tables").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].status", equalTo(404)))
                // Nothing the endpoint's success type would have carried, and nothing deprecated.
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                .andExpect(jsonPath("$.tables").doesNotExist())
                .andExpect(jsonPath("$.pagination").doesNotExist());
    }

    /**
     * The top-level fields of an error body. These tests compare the fields rather than the whole body, because
     * the trace id the advice folds into every error's details differs between two requests.
     */
    private Set<String> errorBodyShapeOf(MockHttpServletRequestBuilder request) throws Exception {
        String body = mockMvc.perform(request.accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(body).properties().stream().map(Map.Entry::getKey).collect(toSet());
    }

    @Test
    public void search_should_returnBadRequest_when_theCredentialsHeaderIsMalformed() throws Exception {
        DataConnectRequest request = new DataConnectRequest();
        request.setSqlQuery("SELECT 1");

        ResultActions resultActions = mockMvc.perform(
                post("/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("GA4GH-Search-Authorization", "userToken")
                        .accept(MediaType.APPLICATION_JSON));

        resultActions
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].status", equalTo(400)))
                .andExpect(jsonPath("$.errors[0].title", Matchers.containsString("name=value")));

        verify(trinoDataConnectAdapter, never()).search(any(), any(), any(), any());
    }

    @Test
    public void deleteSearchQuery_should_returnNotFound_when_theAdapterRejectsThePage() throws Exception {
        String page = "v1/statement/executing/20260902_203359_48519_fnmag/y5bb5cace5500a2cf109b1c50c648b009c40a142f/4";
        String queryJobId = "20260902_203359_48519_fnmag";
        doThrow(new InvalidQueryJobException(queryJobId))
                .when(trinoDataConnectAdapter).deleteQueryJob(anyString(), anyString(), any());

        ResultActions resultActions = mockMvc.perform(
                delete("/search/" + page).param("queryJobId", queryJobId));

        resultActions
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].status", equalTo(404)));
    }

    @Test
    public void deleteSearchQuery_should_returnBadGateway_when_trinoAnswersUnexpectedly() throws Exception {
        String page = "v1/statement/executing/20260902_203359_48519_fnmag/y5bb5cace5500a2cf109b1c50c648b009c40a142f/4";
        String queryJobId = "20260902_203359_48519_fnmag";
        doThrow(new TrinoUnexpectedHttpResponseException(503, "Trino answered 503 when asked to cancel a query."))
                .when(trinoDataConnectAdapter).deleteQueryJob(anyString(), anyString(), any());

        ResultActions resultActions = mockMvc.perform(
                delete("/search/" + page).param("queryJobId", queryJobId));

        resultActions
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].status", equalTo(502)));
    }

    @Test
    public void search_should_relayTheBarePageToTrino_when_theCallerReachedThisServiceThroughAProxyPrefix() throws Exception {
        // The fast path re-reads the nextPageUrl this service generated, and that URL carries
        // X-Forwarded-Prefix. This service offers Trino the page alone, with none of its own prefix on it.
        String page = "v1/statement/executing/test-job-123/y5bb5cace5500a2cf109b1c50c648b009c40a142f/1";
        DataConnectRequest request = new DataConnectRequest();
        request.setSqlQuery("SELECT * FROM test_table");
        TableData queuedPage = new TableData(
                DataModel.builder().ref("http://example.com/ref").build(),
                new ArrayList<>(),
                null,
                new Pagination(null,
                        URI.create("https://publisher.example.com/api/data-connect/search/" + page
                                + "?queryJobId=test-job-123"),
                        null),
                QueryJob.builder().id("test-job-123").build());
        when(trinoDataConnectAdapter.search(anyString(), any(), any(), any())).thenReturn(queuedPage);
        when(trinoDataConnectAdapter.getNextSearchPage(anyString(), anyString(), any(), any()))
                .thenReturn(queuedPage);

        mockMvc.perform(post("/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-Proto", "https")
                        .header("X-Forwarded-Host", "publisher.example.com")
                        .header("X-Forwarded-Prefix", "/api/data-connect")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(trinoDataConnectAdapter, atLeastOnce())
                .getNextSearchPage(eq(page), eq("test-job-123"), any(), any());
    }

    @Test
    public void search_should_retryCallsToTrinoUntilDataIsReturned() throws Exception {
        // Prepare test data
        String testQuery = "SELECT * FROM test_table";
        DataConnectRequest request = new DataConnectRequest();
        request.setSqlQuery(testQuery);
        
        // Create initial response with empty data but next page URL (triggers retry)
        QueryJob queryJob = QueryJob.builder()
                .id("test-job-123")
                .query(testQuery)
                .build();
        
        Pagination paginationWithNext = new Pagination(
                null, 
                URI.create("http://localhost:8080/search/page2"), 
                null
        );
        
        TableData emptyResponse = new TableData(
                DataModel.builder().ref("http://example.com/ref").build(),
                new ArrayList<>(), // Empty data - should trigger retry
                null,
                paginationWithNext,
                queryJob
        );

        // Create final response with actual data (stops retry)
        List<Map<String, Object>> actualData = List.of(
                Map.of("column1", "value1", "column2", 123),
                Map.of("column1", "value2", "column2", 456)
        );
        
        TableData dataResponse = new TableData(
                DataModel.builder().ref("http://example.com/ref").build(),
                actualData,
                null,
                paginationWithNext,
                queryJob
        );
        
        // Mock the adapter behavior
        when(trinoDataConnectAdapter.search(anyString(), any(), any(), any()))
                .thenReturn(emptyResponse);
        
        // Mock subsequent calls to getNextSearchPage
        when(trinoDataConnectAdapter.getNextSearchPage(anyString(), anyString(), any(), any()))
                .thenReturn(emptyResponse)  // First retry - still empty
                .thenReturn(dataResponse);        // Second retry - has data
        
        // Execute the search request
        ResultActions resultActions = mockMvc.perform(post("/search")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
        
        // Verify the response contains the actual data (from the retry)
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].column1", equalTo("value1")))
                .andExpect(jsonPath("$.data[0].column2", equalTo(123)))
                .andExpect(jsonPath("$.data[1].column1", equalTo("value2")))
                .andExpect(jsonPath("$.data[1].column2", equalTo(456)));
        
        // Verify the adapter was called correctly
        verify(trinoDataConnectAdapter).search(eq(testQuery), any(), any(), any());
        
        // Verify getNextSearchPage was called twice (retried until data was found)
        verify(trinoDataConnectAdapter, times(2)).getNextSearchPage(anyString(), anyString(), any(), any());
    }

    @Test
    public void search_should_retryCallsToTrinoUntilEndOfPagination_when_queryReturnsNoData() throws Exception {
        // Prepare test data
        String testQuery = "SELECT * FROM test_table";
        DataConnectRequest request = new DataConnectRequest();
        request.setSqlQuery(testQuery);

        // Create initial response with empty data but next page URL (triggers retry)
        QueryJob queryJob = QueryJob.builder()
                .id("test-job-123")
                .query(testQuery)
                .build();

        Pagination paginationWithNext = new Pagination(
                null,
                URI.create("http://localhost:8080/search/page2"),
                null
        );

        TableData emptyResponse = new TableData(
                DataModel.builder().ref("http://example.com/ref").build(),
                new ArrayList<>(), // Empty data - should trigger retry
                null,
                paginationWithNext,
                queryJob
        );

        TableData noDataNoNextPageResponse = new TableData(
                DataModel.builder().ref("http://example.com/ref").build(),
                null,
                null,
                null,
                queryJob
        );

        // Mock the adapter behavior
        when(trinoDataConnectAdapter.search(anyString(), any(), any(), any()))
                .thenReturn(emptyResponse);

        // Mock subsequent calls to getNextSearchPage
        when(trinoDataConnectAdapter.getNextSearchPage(anyString(), anyString(), any(), any()))
                .thenReturn(emptyResponse)
                .thenReturn(emptyResponse)
                .thenReturn(noDataNoNextPageResponse);

        // Execute the search request
        ResultActions resultActions = mockMvc.perform(post("/search")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));

        // Verify the response contains the actual data (from the retry)
        resultActions
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data", Matchers.nullValue()))
                .andExpect(jsonPath("$.pagination").doesNotExist());

        // Verify the adapter was called correctly
        verify(trinoDataConnectAdapter).search(eq(testQuery), any(), any(), any());

        // Verify getNextSearchPage was called twice (retried until data was found)
        verify(trinoDataConnectAdapter, times(3)).getNextSearchPage(anyString(), anyString(), any(), any());
    }
}
