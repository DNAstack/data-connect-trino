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
    TrinoDataPage query(String statement, Map<String, String> extraCredentials);

    /**
     * Fetches the given page of a running query from Trino (which may be empty, but contain a nextUri
     * link to other results (which also may be empty and contain a nextUri link, etc.)
     *
     * @param page             the next page token returned by Trino in a previous call to {@link #query(String, Map)} or to this method.
     * @param extraCredentials The extra X-Trino-Extra-Credentials to include in the request.
     * @return The first JSON response from Trino that's either a partial result (even with 0 rows), or a final result.
     * Never null.
     */
    TrinoDataPage next(String page, Map<String, String> extraCredentials);

    /**
     * Used by the {@link QueryCleanupManager} to kill queries that are no longer being used.
     *
     * @param nextPageUrl the next page URL returned by Trino in a previous call.
     */
    void killQuery(String nextPageUrl);

    /**
     * Asks Trino to cancel the query addressed by the given page, without interpreting its answer.
     *
     * @param page             the page token returned by Trino in a previous call to {@link #query(String, Map)} or
     *                         {@link #next(String, Map)}. Trino issues each page with a slug of its own and accepts
     *                         the cancellation only for a page it issued, so relaying a page a caller supplied is how
     *                         that caller demonstrates it holds one.
     * @param extraCredentials The extra X-Trino-Extra-Credentials to include in the request.
     * @return the HTTP status Trino answered with. 2xx means the query was cancelled; 404 means Trino does not
     * recognize the page.
     */
    int cancelQuery(String page, Map<String, String> extraCredentials);
}
