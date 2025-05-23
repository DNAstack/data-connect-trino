package com.dnastack.ga4gh.dataconnect.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.net.URI;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageIndexEntry {
    @JsonProperty("description")
    private String catalog; //may need to rename

    @JsonProperty("schema")
    private String schema;

    @JsonProperty("url")
    private URI url;

    @JsonProperty("page")
    private int page;
}
