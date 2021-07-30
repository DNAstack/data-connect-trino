package com.dnastack.ga4gh.search.adapter.test.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.Map;

@Data
@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
public class SearchAuthRequest {
    String key;
    String resourceType;
    Map<String, String> resourceDescription;
}
