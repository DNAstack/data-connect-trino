package com.dnastack.ga4gh.search.adapter.test.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.net.URI;

@Data
public class PageIndexEntry {
    @JsonProperty("description")
    private String description;

    @JsonProperty("url")
    private URI url;

    @JsonProperty("page")
    private int page;
}

