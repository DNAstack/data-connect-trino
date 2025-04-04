package com.dnastack.ga4gh.dataconnect.controller; // Assuming controller package

import com.dnastack.ga4gh.dataconnect.DataConnectTrinoApplication;
import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoDataConnectAdapter;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.TrinoNoSuchCatalogException;
import com.dnastack.ga4gh.dataconnect.model.DataModel;
import com.dnastack.ga4gh.dataconnect.model.Pagination;
import com.dnastack.ga4gh.dataconnect.model.TableInfo;
import com.dnastack.ga4gh.dataconnect.model.TablesList;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
    public void getTables_givenValidRequest_returnOkAndTablesList() throws Exception {
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
    public void getTables_givenAdapterException_returnInternalServerError() throws Exception {
        RuntimeException adapterException = new RuntimeException("Adapter failed");
        when(trinoDataConnectAdapter.getTables(any(), any()))
                .thenThrow(adapterException);

        ResultActions resultActions = mockMvc.perform(get("/tables")
                .accept(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isInternalServerError());

        verify(trinoDataConnectAdapter).getTables(any(), any());
    }

    @Test
    public void getTablesByCatalogAndSchema_givenValidRequest_returnOkAndTablesList() throws Exception {
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
    public void getTablesByCatalogAndSchema_givenMissingCatalog_returnNotFound() throws Exception {
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
    public void getTablesByCatalogAndSchema_givenGenericFailure_returnInternalServerError() throws Exception {
        RuntimeException adapterException = new RuntimeException("Generic adapter failure");
        when(trinoDataConnectAdapter.getTablesByCatalogAndSchema(anyString(), anyString(), any(), any()))
                .thenThrow(adapterException);

        ResultActions resultActions = mockMvc.perform(
                get("/tables/catalog/{catalogName}/schema/{schemaName}", sampleCatalog, sampleSchema)
                        .accept(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isInternalServerError());

        verify(trinoDataConnectAdapter).getTablesByCatalogAndSchema(anyString(), anyString(), any(), any());
    }
}