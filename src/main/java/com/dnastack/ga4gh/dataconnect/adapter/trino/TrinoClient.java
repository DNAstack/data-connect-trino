package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

//TODO: get rid of?
public interface TrinoClient {

    /**
     * Runs the given SQL statement and returns the result from Trino (which may be empty, but contain
     * a nextUri link to other results (which also may be empty and contain a nextUri link, etc.)
     *
     * @param statement        the SQL statement to execute.
     * @param extraCredentials The extra X-Trino-Extra-Credentials to include in the request.
     * @return The first JSON response from Trino that's either a partial result (even with 0 rows), or a final result.
     * Never null.
     */
    JsonNode query(String statement, Map<String, String> extraCredentials);

    /**
     * Fetches the given page of a running query from Trino (which may be empty, but contain a nextUri
     * link to other results (which also may be empty and contain a nextUri link, etc.)
     *
     * @param page             the next page token returned by Trino in a previous call to {@link #query(String, Map)} or to this method.
     * @param extraCredentials The extra X-Trino-Extra-Credentials to include in the request.
     * @return The first JSON response from Trino that's either a partial result (even with 0 rows), or a final result.
     * Never null.
     */
    JsonNode next(String page, Map<String, String> extraCredentials);
}
