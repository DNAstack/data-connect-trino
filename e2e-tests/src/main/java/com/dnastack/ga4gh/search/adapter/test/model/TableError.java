package com.dnastack.ga4gh.search.adapter.test.model;

import lombok.Data;

@Data
public class TableError {
    private String source;
    private int status; // HTTP status
    private String title;
    private String details;
}
