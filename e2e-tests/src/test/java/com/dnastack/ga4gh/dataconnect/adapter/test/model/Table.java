package com.dnastack.ga4gh.dataconnect.adapter.test.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds a Data Connect TableData response.
 */
@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Table {
    private String name;
    private String description;
    private DataModel dataModel;
    private List<Map<String, Object>> data;
    private List<TableError> errors;
    private Pagination pagination;

    // track additional JSON properties so we can see them when we log errors

    @JsonAnySetter
    private Map<String, Object> additionalProperties = new LinkedHashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    public List<Map<String, Object>> getData() {
        if (data == null) {
            data = new ArrayList<>();
        }
        return data;
    }

    public List<TableError> getErrors() {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        return errors;
    }

    public void append(Table tableData) {
        getData().addAll(tableData.getData());
        if (tableData.getDataModel() != null) {
            this.dataModel = tableData.getDataModel();
        }
        getErrors().addAll(tableData.getErrors());
        this.pagination = tableData.getPagination();
    }
}
