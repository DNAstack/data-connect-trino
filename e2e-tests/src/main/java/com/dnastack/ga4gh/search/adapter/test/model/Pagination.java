package com.dnastack.ga4gh.search.adapter.test.model;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

import java.net.URI;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class Pagination {
    private URI nextPageUrl;
    private URI previousPageUrl;
}
