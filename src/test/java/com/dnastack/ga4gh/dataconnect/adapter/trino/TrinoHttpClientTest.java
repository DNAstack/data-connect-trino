package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.dnastack.ga4gh.dataconnect.adapter.security.ServiceAccountAuthenticator;
import com.dnastack.tenancy.context.TenantContextAccessor;
import io.micrometer.tracing.CurrentTraceContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TrinoHttpClientTest {

    private MockWebServer configuredTrinoServer;
    private MockWebServer attackerServer;
    private TrinoHttpClient client;

    @Before
    public void setUp() throws IOException {
        configuredTrinoServer = new MockWebServer();
        configuredTrinoServer.start();
        attackerServer = new MockWebServer();
        attackerServer.start();

        client = new TrinoHttpClient(
            fakeTracer(),
            new OkHttpClient.Builder().readTimeout(Duration.ofSeconds(2)).build(),
            configuredTrinoServer.url("/").toString(),
            new ServiceAccountAuthenticator(),
            new TenantContextAccessor()
        );
    }

    @After
    public void tearDown() throws IOException {
        configuredTrinoServer.shutdown();
        attackerServer.shutdown();
    }

    private static Tracer fakeTracer() {
        Span span = mock(Span.class);
        when(span.name(any())).thenReturn(span);
        when(span.start()).thenReturn(span);

        TraceContext traceContext = mock(TraceContext.class);
        when(traceContext.traceId()).thenReturn("00000000000000000000000000000000");
        when(traceContext.spanId()).thenReturn("0000000000000000");
        CurrentTraceContext currentTraceContext = mock(CurrentTraceContext.class);
        when(currentTraceContext.context()).thenReturn(traceContext);

        Tracer tracer = mock(Tracer.class);
        when(tracer.nextSpan()).thenReturn(span);
        when(tracer.currentTraceContext()).thenReturn(currentTraceContext);
        when(tracer.withSpan(any())).thenReturn(mock(Tracer.SpanInScope.class));
        return tracer;
    }

    @Test
    public void next_should_reject_when_pageIsAnAbsoluteUrl() {
        // An absolute page URL naming a host other than the configured Trino server -- what an attacker relays
        // through the public /search/** endpoint. Every legitimate caller (the adapter, the query cleanup sweep)
        // strips any scheme and host before calling in here, so this client trusts none of them and refuses an
        // absolute-looking page outright rather than silently resolving it.
        String attackerPage = attackerServer.url("/v1/statement/executing/fake-query/slug/1").toString();

        assertThatThrownBy(() -> client.next(attackerPage, Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(attackerPage);

        assertThat(attackerServer.getRequestCount()).isZero();
        assertThat(configuredTrinoServer.getRequestCount()).isZero();
    }
}
