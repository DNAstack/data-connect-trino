package com.dnastack.ga4gh.dataconnect.adapter.test.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ListTableResponse {
    private List<Table> tables;
    private Pagination pagination;
    private List<PageIndexEntry> index;
    private List<Map<String, String>> errors;
    private Map<String, String> error;
}
