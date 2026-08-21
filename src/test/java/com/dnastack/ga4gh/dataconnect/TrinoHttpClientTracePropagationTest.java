package com.dnastack.ga4gh.dataconnect;

import com.dnastack.ga4gh.dataconnect.adapter.security.ServiceAccountAuthenticator;
import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoHttpClient;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.PropagatingSenderTracingObservationHandler;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelPropagator;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that a query submitted to Trino carries the caller's trace context, in both forms it travels in.
 * <p>
 * The DNAstack Trino plugins read the {@code traceparent} extra credential in preference to Trino's own ambient tracing
 * context. That credential is what puts a Trino authorization decision — and the collection-service access check behind
 * it — in the same trace as the query that provoked them (CU-86bb955nw).
 * <p>
 * It has to be the credential for now, because we run Trino with tracing disabled. Trino's {@code tracing.enabled} also
 * turns on trace publishing, which we do not want to pay Datadog for. With it off, Trino reads no {@code traceparent}
 * header at all.
 * <p>
 * The header is asserted here anyway. It comes from the client being observation-instrumented, so a plain
 * {@code new OkHttpClient()} would silently drop it, and it is what Trino would read if tracing were ever enabled
 * (CU-86bbh3xz8).
 */
public class TrinoHttpClientTracePropagationTest {

    private MockWebServer trino;
    private ObservationRegistry observationRegistry;
    private Tracer tracer;

    @Before
    public void setUp() throws IOException {
        trino = new MockWebServer();
        trino.start();

        SdkTracerProvider tracerProvider = SdkTracerProvider.builder().build();
        OpenTelemetrySdk otel = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build();
        io.opentelemetry.api.trace.Tracer otelTracer = otel.getTracer("test");

        tracer = new OtelTracer(otelTracer, new OtelCurrentTraceContext(), event -> {});
        observationRegistry = ObservationRegistry.create();
        observationRegistry.observationConfig().observationHandler(
            new PropagatingSenderTracingObservationHandler<>(tracer, new OtelPropagator(otel.getPropagators(), otelTracer)));
    }

    @After
    public void tearDown() throws IOException {
        trino.shutdown();
    }

    @Test
    public void trinoQuery_should_carryTheCallersTraceInATraceparentExtraCredential() throws Exception {
        trino.enqueue(new MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("{\"id\":\"20260820_000000_00000_aaaaa\",\"columns\":[],\"data\":[],"
                + "\"stats\":{\"state\":\"FINISHED\"}}"));

        Span callersSpan = tracer.nextSpan().name("dataConnectRequest").start();
        String traceId = callersSpan.context().traceId();
        try (Tracer.SpanInScope inScope = tracer.withSpan(callersSpan)) {
            new TrinoHttpClient(tracer, trinoHttpClient(), trino.url("/").toString(), new ServiceAccountAuthenticator())
                .query("SELECT 1", Map.of());
        } finally {
            callersSpan.end();
        }

        RecordedRequest recorded = trino.takeRequest();
        assertThat(recorded.getHeaders().values("X-Trino-Extra-Credential"))
            .as("the Trino plugins read the caller's trace from an extra credential, not from the traceparent header")
            .anySatisfy(credential -> assertThat(credential).startsWith("traceparent=00-" + traceId + "-"));
    }

    @Test
    public void trinoRequest_should_carryTheCallersTraceInATraceparentHeader() throws Exception {
        trino.enqueue(new MockResponse().setResponseCode(200).setBody("{}"));
        OkHttpClient client = trinoHttpClient();

        Span callersSpan = tracer.nextSpan().name("dataConnectRequest").start();
        String traceId = callersSpan.context().traceId();
        try (Tracer.SpanInScope inScope = tracer.withSpan(callersSpan)) {
            try (Response response = client.newCall(
                new Request.Builder().url(trino.url("/v1/statement")).build()).execute()) {
                assertThat(response.code()).isEqualTo(200);
            }
        } finally {
            callersSpan.end();
        }

        RecordedRequest recorded = trino.takeRequest();
        assertThat(recorded.getHeader("traceparent"))
            .as("Trino has nothing to inherit the caller's trace from without this header")
            .isNotNull()
            .contains(traceId);
    }

    /** The Trino client exactly as the application builds it. */
    private OkHttpClient trinoHttpClient() {
        return new ApplicationConfig(null, "", Set.of(), trino.url("/").toString())
            .httpClient(observationRegistry);
    }
}
