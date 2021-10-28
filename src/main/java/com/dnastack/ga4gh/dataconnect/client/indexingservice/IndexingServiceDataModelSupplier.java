package com.dnastack.ga4gh.dataconnect.client.indexingservice;

import com.dnastack.ga4gh.dataconnect.DataModelSupplier;
import com.dnastack.ga4gh.dataconnect.model.DataModel;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IndexingServiceDataModelSupplier implements DataModelSupplier {

    private final IndexingServiceClient client;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);


    private final String publisherDataCatalogName;

    public IndexingServiceDataModelSupplier(IndexingServiceClient client, String publisherDataCatalogName) {
        this.client = client;
        this.publisherDataCatalogName = publisherDataCatalogName;
    }

    @Override
    public DataModel supply(String tableName) {
        String jsonSchemaAsString;
        if (publisherDataCatalogName != null && tableName.startsWith(publisherDataCatalogName + ".") ) {
            tableName = tableName.substring(publisherDataCatalogName.length() + 1);
        }

        try {
            final LibraryItem libraryItem = client.get(tableName);
            log.info("LibraryItem {}", libraryItem);

            jsonSchemaAsString = libraryItem.getJsonSchema();
            log.info("jsonSchemaAsString {}", jsonSchemaAsString);
        } catch (FeignException.NotFound ignored) {
            log.info("Item not found in library table");
            return null;
        }
        try {
            return objectMapper.readValue(jsonSchemaAsString, DataModel.class);
        } catch (Exception e) {
            log.error("Failed to convert {} to DataModel", jsonSchemaAsString, e);
            return null;
        }
    }

}
