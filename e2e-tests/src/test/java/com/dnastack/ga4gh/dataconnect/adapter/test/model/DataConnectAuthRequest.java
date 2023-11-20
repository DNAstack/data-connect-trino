package com.dnastack.ga4gh.dataconnect.adapter.test.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.Map;

@Data
@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
public class DataConnectAuthRequest {
    String key;
    String resourceType;
    Map<String, String> resourceDescription;
}
