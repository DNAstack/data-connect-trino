package com.dnastack.ga4gh.dataconnect.adapter.test.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.util.Map;

@Data
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public class DataConnectAuthRequest {
    String key;
    String resourceType;
    Map<String, String> resourceDescription;
}
