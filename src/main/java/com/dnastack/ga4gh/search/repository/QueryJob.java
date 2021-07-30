package com.dnastack.ga4gh.search.repository;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QueryJob {
    @Id
    private String id;

    private String query;

    // This column is used to store table schema retrieved from tables-registry.
    // If the table schema is not available in tables-registry, this column stays empty.
    private String schema;
}