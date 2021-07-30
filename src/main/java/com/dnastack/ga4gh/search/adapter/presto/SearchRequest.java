package com.dnastack.ga4gh.search.adapter.presto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SearchRequest {
    @JsonProperty("query")
    private String sqlQuery;
}
