package com.dnastack.ga4gh.dataconnect.adapter.telemetry;

import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoClient;
import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoDataPage;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.TimeUnit;

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

    public TrinoTelemetryClient(TrinoClient client, MeterRegistry registry) {
        this.client = client;

        this.queryCount = Counter.builder("search.queries.queries_performed")
                .description("The raw number of queries performed over a given step of time.")
                .register(registry);

        this.queryLatency = Timer.builder("search.queries.query_latency")
                .description("The average latency of queries performed over a given step of time.")
                .register(registry);

        this.queryQueueTime = Timer.builder("search.queries.queue_time")
                .description( "The average time a request spends in the queued state")
                .register(registry);

        this.queryExecuteTime = Timer.builder("search.queries.execute_time")
                .description( "The average time a request spends executing")
                .register(registry);

        this.pageCount = Counter.builder("search.queries.additional_pages_retrieved")
                .description("The number of additional pages retrieved after an initial query.")
                .register(registry);

        stateCache = CacheBuilder.newBuilder().expireAfterWrite(15, TimeUnit.MINUTES).build();
    }

    public TrinoDataPage query(String statement, Map<String, String> extraCredentials) {
        queryCount.increment();
        long start = System.currentTimeMillis();
        TrinoDataPage jn = client.query(statement, extraCredentials);
        queryLatency.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
        traceQueryPerformance(jn, start);
        return jn;
    }

    public TrinoDataPage next(String page, Map<String, String> extraCredentials) {
        queryCount.increment();
        long start = System.currentTimeMillis();
        TrinoDataPage trinoDataPage = client.next(page, extraCredentials);
        queryLatency.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);
        traceQueryPerformance(trinoDataPage,start);
        return trinoDataPage;
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

    private void traceQueryPerformance(TrinoDataPage trinoDataPage, long currentTime) {
        try {
            String jobId = trinoDataPage.id();
            String infoUri = trinoDataPage.infoUri();
            String state = trinoDataPage.stats().state();

            TrinoState previousState = stateCache.getIfPresent(jobId);
            if (previousState == null) {
                log.info("Trino query '" + jobId + "' info: " + infoUri);
                stateCache.put(jobId, new TrinoState(state, currentTime));
            } else if (!previousState.getState().equals(state)) {
                if (previousState.getState().equals("QUEUED") && state.equals("RUNNING")) {
                    queryQueueTime.record(currentTime - previousState.getLastTransitionTime(), TimeUnit.MILLISECONDS);
                } else if (previousState.getState().equals("RUNNING") && state.equals("FINISHED")){
                    queryExecuteTime.record(currentTime - previousState.getLastTransitionTime(),TimeUnit.MILLISECONDS);
                }

                stateCache.put(jobId, new TrinoState(state, currentTime));
            }
        } catch (Exception e){
            log.error("Encountered an error while tracing query performance: " + e.getMessage(),e);
        }

    }
}
