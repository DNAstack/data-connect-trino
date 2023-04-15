package com.dnastack.ga4gh.dataconnect.client.collectionservice;

import com.dnastack.ga4gh.dataconnect.DataModelSupplier;
import com.dnastack.ga4gh.dataconnect.model.DataModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.FeignException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CollectionServiceDataModelSupplier implements DataModelSupplier {

    private final CollectionServiceClient client;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);


    private final String collectionsCatalogName;

    public CollectionServiceDataModelSupplier(
            @NonNull CollectionServiceClient client,
            @NonNull String collectionsCatalogName) {
        this.client = client;
        this.collectionsCatalogName = collectionsCatalogName;
    }

    @Override
    public DataModel supply(String fullyQualifiedTableName) {
        String[] tableNameParts = fullyQualifiedTableName.split("\\.", 3);
        String catalogName = tableNameParts[0]; // must match this.collectionsCatalogName
        String schemaName = tableNameParts[1];  // the collection's dbSchemaName
        String tableName = tableNameParts[2];   // the table's displayName

        if (!catalogName.equals(collectionsCatalogName)) {
            log.debug("Table not in '{}' catalog. Can't supply data model.", collectionsCatalogName);
            return null;
        }

        final CollectionItem collectionItem;
        try {
            collectionItem = client.getItem(schemaName, tableName);
            log.debug("{} CollectionItem is {}", fullyQualifiedTableName, collectionItem);
            return collectionItem.getJsonSchema();
        } catch (FeignException e) {
            log.warn("Failed to fetch collection item for {} -- returning null data model", fullyQualifiedTableName, e);
            return null;
        }
    }

}
