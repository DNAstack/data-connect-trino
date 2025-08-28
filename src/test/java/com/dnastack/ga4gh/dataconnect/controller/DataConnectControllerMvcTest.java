package com.dnastack.ga4gh.dataconnect.controller;

import com.dnastack.ga4gh.dataconnect.DataConnectTrinoApplication;
import com.dnastack.ga4gh.dataconnect.adapter.trino.DataConnectRequest;
import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoDataConnectAdapter;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.TrinoNoSuchCatalogException;
import com.dnastack.ga4gh.dataconnect.model.*;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY;
import static io.zonky.test.db.AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
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

    @MockBean
    private TrinoDataConnectAdapter trinoDataConnectAdapter;

    @MockBean
    private Jdbi jdbi; // Mock the JDBI instance if used


    private TablesList sampleTablesList;
    private String sampleCatalog = "test_catalog";
    private String sampleSchema = "test_schema";
    private String sampleTable = "table1";
    private String sampleQualifiedTableName = sampleCatalog + "." + sampleSchema + "." + sampleTable;
    private String sampleCredentialsHeader = "Bearer abc";
    private Map<String, String> expectedCredentialsMap = Map.of("Authorization", sampleCredentialsHeader);

    @Before
    public void setUp() {
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
    public void getTables_should_returnInternalServerError_when_adapterThrowsException() throws Exception {
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
    public void getTablesByCatalogAndSchema_should_returnNotFound_when_catalogNotFound() throws Exception {
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
    public void getTablesByCatalogAndSchema_returnInternalServerError_when_adapterThrowsException() throws Exception {
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