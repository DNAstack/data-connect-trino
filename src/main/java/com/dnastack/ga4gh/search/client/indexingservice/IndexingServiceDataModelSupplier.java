package com.dnastack.ga4gh.search.client.indexingservice;

import com.dnastack.ga4gh.search.DataModelSupplier;
import com.dnastack.ga4gh.search.model.DataModel;
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

    public IndexingServiceDataModelSupplier(IndexingServiceClient client) {
        this.client = client;
    }

    @Override
    public DataModel supply(String tableName) {
        String jsonSchemaAsString;
        try {
            final LibraryItem libraryItem = client.get(tableName);
            jsonSchemaAsString = libraryItem.getJsonSchema();
        } catch (FeignException.NotFound ignored) {
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
