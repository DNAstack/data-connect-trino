package com.dnastack.ga4gh.dataconnect.adapter.trino;

import java.util.Comparator;

public record CatalogWithSchema(String catalogName, String schema) implements Comparable<CatalogWithSchema> {
    @Override
    public int compareTo(CatalogWithSchema o) {

        return Comparator.comparing(CatalogWithSchema::catalogName)
                .thenComparing(CatalogWithSchema::schema)
                .compare(this, o);
    }
}
