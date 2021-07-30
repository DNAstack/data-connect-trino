package com.dnastack.ga4gh.search.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class DataModel {
    @JsonProperty("$id")
    private URI id;

    @JsonProperty("description")
    private String description;

    @JsonProperty("$schema")
    private URI schema;

    @JsonProperty("properties")
    private Map<String, ColumnSchema> properties;

    @JsonProperty("$ref")
    private String ref;

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

    @JsonIgnore
    public boolean isUsable() {
        return (properties != null && !properties.isEmpty())
            || (ref != null && !ref.isEmpty());
    }
}
