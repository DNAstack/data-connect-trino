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
public class TablesList {

    @JsonProperty("tables")
    private List<TableInfo> tableInfos;

    @JsonProperty("errors")
    private List<TableError> errors;

    @Deprecated
    @JsonProperty("error")
    private TableError error;

    @JsonProperty("pagination")
    private Pagination pagination;

    @JsonProperty("index")
    private List<PageIndexEntry> index;

    public TablesList(List<TableInfo> tableInfos, TableError error, Pagination pagination) {
        this.tableInfos = tableInfos;
        if (error != null) {
            this.errors = List.of(error);
        }
        this.error = error;
        this.pagination = pagination;
    }

    public static TablesList errorInstance(TableError tableError) {
        return new TablesList(null, tableError, null);
    }
}
