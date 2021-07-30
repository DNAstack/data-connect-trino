package com.dnastack.ga4gh.search.adapter.test.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ColumnSchema {
    @JsonProperty("items")
    private ColumnSchema items;

    @JsonProperty("format")
    private String format;

    @JsonProperty("type")
    private String type;

    @JsonProperty("$comment")
    private String comment;

    @JsonProperty("$ref")
    private String ref;

    @JsonProperty("properties")
    private Map<String, ColumnSchema> properties;

    @JsonIgnore
    private final Map<String, Object> additionalProperties = new LinkedHashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String key, Object value) {
        additionalProperties.put(key, value);
    }
}
