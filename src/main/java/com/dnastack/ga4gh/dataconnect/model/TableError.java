package com.dnastack.ga4gh.dataconnect.model;

import java.util.Map;

import lombok.Data;

@Data
public class TableError {
    private String source;
    private int status; // HTTP status
    private String title;
    private String details;
}
