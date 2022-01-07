package com.dnastack.ga4gh.dataconnect.adapter.telemetry;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/***
 * Wraps a TrinoClient to add telemetry.
 */
@Slf4j
public class TrinoTelemetryClient implements TrinoClient {

    protected final Counter queryCount;
    protected final Counter pageCount;
    protected final Timer queryLatency;
    private final Timer queryQueueTime;
    private final Timer queryExecuteTime;
    private final TrinoClient client;
    private final Cache<String, TrinoState> stateCache;

    public TrinoTelemetryClient(TrinoClient client) {
        this.client = client;
        this.queryCount = Monitor.registerCounter("search.queries.queries_performed",
            "The raw number of queries performed over a given step of time.");
        this.queryLatency = Monitor.registerRequestTimer("search.queries.query_latency",
            "The average latency of queries performed over a given step of time.");
        this.queryQueueTime = Monitor
            .registerRequestTimer("search.queries.queue_time", "The average time a request spends in the queued state");
        this.queryExecuteTime = Monitor
            .registerRequestTimer("search.queries.execute_time", "The average time a request spends executing");
        this.pageCount = Monitor.registerCounter("search.queries.additional_pages_retrieved",
            "The number of additional pages retrieved after an initial query.");
        stateCache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();
    }

    public JsonNode query(String statement, Map<String, String> extraCredentials) {
        queryCount.increment();
        long start = System.currentTimeMillis();
        JsonNode jn = client.query(statement, extraCredentials);
        queryLatency.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
        traceQueryPerformance(jn, start);
        return jn;
    }

    public JsonNode next(String page, Map<String, String> extraCredentials) {
        queryCount.increment();
        long start = System.currentTimeMillis();
        JsonNode jsonNode = client.next(page, extraCredentials);
        queryLatency.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
        traceQueryPerformance(jsonNode,start);
        return jsonNode;
    }

    @Override
    public void killQuery(String nextPageUrl) {
        client.killQuery(nextPageUrl);
    }

    @Data
    @AllArgsConstructor
    private static class TrinoState {

        String state;
        Long lastTransitionTime;
    }

    private void traceQueryPerformance(JsonNode jsonNode, long currentTime) {
        try {
            String jobId = jsonNode.get("id").asText();
            String infoUri = jsonNode.get("infoUri").asText();
            JsonNode stats = jsonNode.get("stats");
            String state = stats.get("state").asText();

            TrinoState previousState = stateCache.getIfPresent(jobId);
            if (previousState == null) {
                log.info("Trino query '" + jobId + "' info: " + infoUri);
                stateCache.put(jobId, new TrinoState(state, currentTime));
            } else if (!previousState.getState().equals(state)) {
                if (previousState.getState().equals("QUEUED") && state.equals("RUNNING")) {
                    queryQueueTime.record(currentTime - previousState.getLastTransitionTime(), TimeUnit.MILLISECONDS);
                } if (previousState.getState().equals("RUNNING") && state.equals("FINISHED")){
                    queryExecuteTime.record(currentTime - previousState.getLastTransitionTime(),TimeUnit.MILLISECONDS);
                }

                stateCache.put(jobId, new TrinoState(state, currentTime));
            }
        } catch (Exception e){
            log.error("Encountered an error while tracing query performance: " + e.getMessage(),e);
        }

    }
}
