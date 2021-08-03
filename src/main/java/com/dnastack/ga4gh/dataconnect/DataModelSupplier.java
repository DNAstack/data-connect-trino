package com.dnastack.ga4gh.dataconnect;

import com.dnastack.ga4gh.dataconnect.model.DataModel;

public interface DataModelSupplier {
    /**
     * Provides the data model of a given table.
     *
     * @param tableName the name of the target table
     * @return The supplier returns null if the table is not found.
     */
    DataModel supply(String tableName);
}
