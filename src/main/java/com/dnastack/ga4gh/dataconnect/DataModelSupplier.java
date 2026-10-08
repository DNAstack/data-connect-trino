package com.dnastack.ga4gh.dataconnect;

import com.dnastack.ga4gh.dataconnect.model.DataModel;
import com.dnastack.tenancy.context.TenantId;

public interface DataModelSupplier {
    /**
     * Provides the data model of a given table.
     *
     * @param tenantId  the tenant the table belongs to
     * @param tableName the name of the target table
     * @return The supplier returns null if the table is not found.
     */
    DataModel supply(TenantId tenantId, String tableName);
}
