package com.dnastack.ga4gh.search.adapter.trino;

import com.dnastack.ga4gh.search.model.ColumnSchema;

public interface Ga4ghTypeTransformer {
    String transform(ColumnSchema columnSchema);
}
