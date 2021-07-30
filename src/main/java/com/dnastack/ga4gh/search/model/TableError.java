package com.dnastack.ga4gh.search.model;

import java.util.Map;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class TableError {
    private String source;
    private int status; // HTTP status
    private String title;
    private String details;
}
