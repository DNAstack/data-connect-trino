package com.dnastack.ga4gh.dataconnect.repository;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString
@Builder
public class QueryJob {
    private String id;

    /**
     * The tenant this query job was created in. A plain UUID rather than a {@code TenantId} because it is an
     * entity field bound straight to the column; the DAO takes the tenant to filter on as a {@code TenantId}.
     */
    private UUID tenantId;

    private String originalTraceId;

    private String query;

    // This column is used to store table schema retrieved from tables-registry.
    // If the table schema is not available in tables-registry, this column stays empty.
    private String schema;

    private Instant startedAt;

    private Instant finishedAt;

    private Instant lastActivityAt;

    private String nextPageUrl;
}
