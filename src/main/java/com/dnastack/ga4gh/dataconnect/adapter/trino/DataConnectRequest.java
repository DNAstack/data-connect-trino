package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.dnastack.audit.util.AuditIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DataConnectRequest {
    @AuditIgnore
    @JsonProperty("query")
    private String sqlQuery;
}
