package com.dnastack.ga4gh.search.adapter.presto;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

//TODO: get rid of?
public interface PrestoClient {

    /**
     * Runs the given SQL statement and returns the result from Presto (which may be empty, but contain
     * a nextUri link to other results (which also may be empty and contain a nextUri link, etc.)
     *
     * @param statement        the SQL statement to execute.
     * @param extraCredentials The extra X-Presto-Extra-Credentials to include in the request.
     * @return The first JSON response from Presto that's either a partial result (even with 0 rows), or a final result.
     * Never null.
     */
    JsonNode query(String statement, Map<String, String> extraCredentials);

    /**
     * Fetches the given page of a running query from Presto (which may be empty, but contain a nextUri
     * link to other results (which also may be empty and contain a nextUri link, etc.)
     *
     * @param page             the next page token returned by Presto in a previous call to {@link #query(String, Map)} or to this method.
     * @param extraCredentials The extra X-Presto-Extra-Credentials to include in the request.
     * @return The first JSON response from Presto that's either a partial result (even with 0 rows), or a final result.
     * Never null.
     */
    JsonNode next(String page, Map<String, String> extraCredentials);
}
