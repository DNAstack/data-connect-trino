package com.dnastack.ga4gh.dataconnect.client.collectionservice;

import feign.Param;
import feign.RequestLine;

public interface CollectionServiceClient {
    @RequestLine("GET /collection/{collectionIdOrSlugNameOrDbSchemaName}/item/{itemIdOrDisplayName}")
    CollectionItem getItem(
            @Param("collectionIdOrSlugNameOrDbSchemaName") String collectionIdOrSlugNameOrDbSchemaName,
            @Param("itemIdOrDisplayName") String itemIdOrDisplayName);
}
