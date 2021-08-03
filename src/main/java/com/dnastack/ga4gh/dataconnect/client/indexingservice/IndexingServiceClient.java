package com.dnastack.ga4gh.dataconnect.client.indexingservice;

import feign.Param;
import feign.RequestLine;

public interface IndexingServiceClient {
    @RequestLine("GET /library/{idOrPreferredName}")
    LibraryItem get(@Param("idOrPreferredName") String idOrPreferredName);
}
