package com.dnastack.ga4gh.dataconnect.adapter.test.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URI;

@Data
public class PageIndexEntry {
    @JsonProperty("description")
    private String description;

    @JsonProperty("schema")
    private String schema;

    @JsonProperty("url")
    private URI url;

    @JsonProperty("page")
    private int page;
}

