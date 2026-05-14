package com.dnastack.ga4gh.dataconnect.adapter.shared;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Value;

import java.util.Map;

@Value
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public class DataConnectAuthRequest {
    String key;
    String resourceType;
    Map<String, String> resourceDescription;
}
