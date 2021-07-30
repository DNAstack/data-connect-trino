package com.dnastack.ga4gh.search.adapter.test.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Table Data
 *
 * This is equivalent to the TableData class
 */
@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
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

    private static <T> List<T> concat(List<T> l1, List<T> l2) {
        if (l1 != null && l2 != null) {
            List<T> result = new ArrayList<>(l1.size() + l2.size());
            result.addAll(l1);
            result.addAll(l2);
            return result;
        } else if (l1 != null) {
            return List.copyOf(l1);
        } else if (l2 != null) {
            return List.copyOf(l2);
        } else {
            return null;
        }
    }

    public void append(Table tableData) {
        if (tableData != null) {
            if (tableData.getData() != null) {
                this.data = concat(this.data, tableData.getData());
            }
            if (tableData.getDataModel() != null) {
                this.dataModel = tableData.getDataModel();
            }
            this.pagination = tableData.getPagination();
        }

    }
}
