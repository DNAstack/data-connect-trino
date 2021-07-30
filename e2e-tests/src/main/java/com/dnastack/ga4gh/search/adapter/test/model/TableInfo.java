package com.dnastack.ga4gh.search.adapter.test.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TableInfo implements Comparable<TableInfo> {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    @JsonProperty("data_model")
    private DataModel dataModel;

    @JsonProperty("errors")
    private List<TableError> errors;

    @Override
    public int compareTo(TableInfo o) {
        return this.name.compareTo(o.name);
    }

}
