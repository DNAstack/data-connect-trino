package com.dnastack.ga4gh.search.adapter.test.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.KebabCaseStrategy.class)
public class SearchAuthChallengeBody {
    SearchAuthRequest authorizationRequest;
}
