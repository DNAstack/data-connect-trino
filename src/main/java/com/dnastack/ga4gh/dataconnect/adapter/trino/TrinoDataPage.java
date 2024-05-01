package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.fasterxml.jackson.databind.JsonNode;

public record TrinoDataPage(
    String id,
    String infoUri,
    String nextUri,
    TrinoError error,
    JsonNode columns,
    JsonNode data,
    TrinoRequestStats stats
) {}
