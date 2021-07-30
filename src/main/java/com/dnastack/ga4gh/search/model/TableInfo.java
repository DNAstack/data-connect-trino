package com.dnastack.ga4gh.search.model;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TableInfo implements Comparable<TableInfo> {

    @JsonProperty("name")
    private String name;

    @JsonProperty("description")
    private String description;

    //private Map<String, Object> dataModel;
    @JsonProperty("data_model")
    private DataModel dataModel;

    @JsonProperty("errors")
    private List<TableError> errors;

    @Override
    public int compareTo(TableInfo o) {
        return this.name.compareTo(o.name);
    }

    public static TableInfo errorInstance(TableError error) {
        return new TableInfo(null, null, null, List.of(error));
    }
}
