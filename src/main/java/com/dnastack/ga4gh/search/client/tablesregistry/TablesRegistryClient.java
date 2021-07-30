package com.dnastack.ga4gh.search.client.tablesregistry;

import com.dnastack.ga4gh.search.client.tablesregistry.model.ListTableRegistryEntry;
import feign.Param;
import feign.RequestLine;

@Deprecated(since = "2021-06-01 per #177369206")
public interface TablesRegistryClient {
    @RequestLine("GET /api/registry/{userId}/collections?table_identifier={tableIdentifier}")
    ListTableRegistryEntry getTableRegistryEntry(@Param("userId") String userId,
                                                 @Param("tableIdentifier") String tableIdentifier);
}
