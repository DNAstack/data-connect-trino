package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class DataConnectRequest {
    @JsonProperty("query")
    private String sqlQuery;
}
