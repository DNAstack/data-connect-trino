package com.dnastack.ga4gh.search.client.tablesregistry;

import com.dnastack.ga4gh.search.DataModelSupplier;
import com.dnastack.ga4gh.search.client.tablesregistry.model.ListTableRegistryEntry;
import com.dnastack.ga4gh.search.model.DataModel;

@Deprecated(since = "2021-06-01 per #177369206")
public class TablesRegistryDataModelSupplier implements DataModelSupplier {
    private final TablesRegistryClient tablesRegistryClient;
    private final OAuthClientConfig oAuthClientConfig;

    public TablesRegistryDataModelSupplier(TablesRegistryClient tablesRegistryClient, OAuthClientConfig oAuthClientConfig) {
        this.tablesRegistryClient = tablesRegistryClient;
        this.oAuthClientConfig = oAuthClientConfig;
    }

    @Override
    public DataModel supply(String tableName) {
        ListTableRegistryEntry registryEntry = tablesRegistryClient.getTableRegistryEntry(oAuthClientConfig.getClientId(), tableName);

        if (registryEntry == null || registryEntry.getTableCollections().isEmpty()) {
            return null;
        }

        return registryEntry.getTableCollections().get(0).getTableSchema();
    }
}
