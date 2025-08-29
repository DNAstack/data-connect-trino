package com.dnastack.ga4gh.dataconnect.adapter.test.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.net.URI;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Pagination {
    private URI nextPageUrl;
    private URI previousPageUrl;
}
