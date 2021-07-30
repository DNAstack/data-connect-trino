package com.dnastack.ga4gh.search.adapter.shared;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.Value;

import java.util.Map;

@Value
@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
public class SearchAuthRequest {
    String key;
    String resourceType;
    Map<String, String> resourceDescription;
}
