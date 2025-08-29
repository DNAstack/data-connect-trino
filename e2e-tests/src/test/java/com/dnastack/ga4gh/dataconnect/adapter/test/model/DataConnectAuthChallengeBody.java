package com.dnastack.ga4gh.dataconnect.adapter.test.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.KebabCaseStrategy.class)
public class DataConnectAuthChallengeBody {
    DataConnectAuthRequest authorizationRequest;
}
