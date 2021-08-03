package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.dnastack.ga4gh.dataconnect.model.ColumnSchema;

public interface Ga4ghTypeTransformer {
    String transform(ColumnSchema columnSchema);
}
