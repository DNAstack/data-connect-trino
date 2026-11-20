package com.dnastack.ga4gh.dataconnect.model;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoError;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.TrinoInternalErrorException;
import com.dnastack.ga4gh.dataconnect.adapter.trino.exception.TrinoUnknownTenantException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TableErrorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TrinoError trinoError(String errorName, String errorType) throws Exception {
        return objectMapper.readValue("""
            {"message":"some message","errorCode":1,"errorName":"%s","errorType":"%s",
             "failureInfo":{"type":"io.trino.spi.TrinoException","message":"some message","stack":[]}}"""
            .formatted(errorName, errorType), TrinoError.class);
    }

    @Test
    public void fromThrowable_should_giveA503WithTheUnknownTenantType_when_trinoReportsUnknownTenant() throws Exception {
        TableError error = TableError.fromThrowable(
            new TrinoUnknownTenantException(trinoError("UNKNOWN_TENANT", "EXTERNAL")), null);

        assertThat(error.getStatus()).isEqualTo(503);
        assertThat(error.getType()).isEqualTo("urn:dnastack:tenancy:unknown-tenant");
    }

    @Test
    public void fromThrowable_should_leaveTheTypeOut_when_trinoReportsAnotherError() throws Exception {
        TableError error = TableError.fromThrowable(
            new TrinoInternalErrorException(trinoError("GENERIC_INTERNAL_ERROR", "INTERNAL_ERROR")), null);

        assertThat(error.getStatus()).isEqualTo(500);
        assertThat(error.getType()).isNull();
        assertThat(objectMapper.writeValueAsString(error)).doesNotContain("\"type\"");
    }
}
