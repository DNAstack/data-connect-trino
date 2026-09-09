package com.dnastack.ga4gh.dataconnect.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the claim {@link DataConnectErrorResponse} rests on: that one body is a valid instance of every type this
 * service's endpoints promise, so a client deserializing the response into the type its endpoint returns finds
 * the errors where that type declares them. If one of those types ever renames or retypes its {@code errors}
 * field, this is what says so.
 */
public class DataConnectErrorResponseTest {

    /** Strict on unknown fields on purpose: a field this body carries that a response type cannot hold fails. */
    private final ObjectMapper objectMapper = new ObjectMapper()
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private String errorBody() throws Exception {
        TableError error = new TableError();
        error.setStatus(404);
        error.setTitle("no such table");
        error.setDetails("a trace id: some detail");
        return objectMapper.writeValueAsString(DataConnectErrorResponse.of(error));
    }

    @Test
    public void errorBody_should_carryNothingButTheErrors() throws Exception {
        assertThat(objectMapper.readTree(errorBody()).properties())
            .as("the top-level fields of an error body")
            .singleElement()
            .extracting(Map.Entry::getKey)
            .isEqualTo("errors");
    }

    @Test
    public void errorBody_should_readBackAsTheTypeTheSearchEndpointsReturn() throws Exception {
        TableData tableData = objectMapper.readValue(errorBody(), TableData.class);

        assertThat(tableData.getErrors())
            .as("the errors of an error body read as TableData")
            .singleElement()
            .extracting(TableError::getTitle)
            .isEqualTo("no such table");
    }

    @Test
    public void errorBody_should_readBackAsTheTypeTheTableListingsReturn() throws Exception {
        TablesList tablesList = objectMapper.readValue(errorBody(), TablesList.class);

        assertThat(tablesList.getErrors())
            .as("the errors of an error body read as TablesList")
            .singleElement()
            .extracting(TableError::getTitle)
            .isEqualTo("no such table");
    }

    @Test
    public void errorBody_should_readBackAsTheTypeTheTableInfoEndpointReturns() throws Exception {
        TableInfo tableInfo = objectMapper.readValue(errorBody(), TableInfo.class);

        assertThat(tableInfo.getErrors())
            .as("the errors of an error body read as TableInfo")
            .singleElement()
            .extracting(TableError::getTitle)
            .isEqualTo("no such table");
    }

    @Test
    public void errorBody_should_leaveEveryOtherFieldOfTheTypeEmpty() throws Exception {
        TableData tableData = objectMapper.readValue(errorBody(), TableData.class);

        assertThat(tableData)
            .as("a failed request read as the type its endpoint returns")
            .extracting(TableData::getData, TableData::getDataModel, TableData::getPagination)
            .containsOnlyNulls();
    }
}
