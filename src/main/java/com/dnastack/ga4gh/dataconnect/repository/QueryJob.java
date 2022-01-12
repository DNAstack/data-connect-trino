package com.dnastack.ga4gh.dataconnect.repository;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
@Builder
public class QueryJob {
    private String id;

    private String query;

    // This column is used to store table schema retrieved from tables-registry.
    // If the table schema is not available in tables-registry, this column stays empty.
    private String schema;

    private Instant startedAt;

    private Instant finishedAt;

    private Instant lastActivityAt;

    private String nextPageUrl;
}
