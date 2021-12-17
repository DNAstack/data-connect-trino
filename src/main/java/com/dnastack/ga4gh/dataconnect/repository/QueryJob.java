package com.dnastack.ga4gh.dataconnect.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
}
