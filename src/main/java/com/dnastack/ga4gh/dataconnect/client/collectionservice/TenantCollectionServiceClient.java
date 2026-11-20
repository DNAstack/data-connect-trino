package com.dnastack.ga4gh.dataconnect.client.collectionservice;

import com.dnastack.tenancy.context.TenantId;
import feign.Param;
import feign.RequestLine;

/**
 * The collection-service calls that act as one tenant, named by the {@code /tenants/{tenantId}} prefix of the path.
 */
public interface TenantCollectionServiceClient {
    @RequestLine("GET /tenants/{tenantId}/collection/{collectionIdOrSlugNameOrDbSchemaName}/item/{itemIdOrDisplayName}")
    CollectionItem getItem(
            @Param("tenantId") TenantId tenantId,
            @Param("collectionIdOrSlugNameOrDbSchemaName") String collectionIdOrSlugNameOrDbSchemaName,
            @Param("itemIdOrDisplayName") String itemIdOrDisplayName);
}
