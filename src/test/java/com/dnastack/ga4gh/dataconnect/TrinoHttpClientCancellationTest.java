package com.dnastack.ga4gh.dataconnect;

import com.dnastack.ga4gh.dataconnect.adapter.security.ServiceAccountAuthenticator;
import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoHttpClient;
import com.dnastack.tenancy.context.TenantContextAccessor;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.otel.bridge.OtelCurrentTraceContext;
import io.micrometer.tracing.otel.bridge.OtelTracer;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies how we relay query cancellation requests to Trino. {@link TrinoHttpClient#cancelQuery(String, Map)} only
 * ever accepts a page relative to the configured Trino server: callers that might hold an absolute URL (the query
 * cleanup sweep, replaying a stored {@code next_page_url}) strip it to a path themselves before calling in here, so
 * this client can refuse to honor a host any caller supplies.
 */
public class TrinoHttpClientCancellationTest {

    private static final String PAGE_PATH = "/v1/statement/executing/20260903_000000_00000_aaaaa/slug/2";

    private MockWebServer trino;
    private OpenTelemetrySdk otel;
    private Tracer tracer;
    private final TenantContextAccessor tenantContextAccessor = new TenantContextAccessor();

    @Before
    public void setUp() throws IOException {
        trino = new MockWebServer();
        trino.start();

        otel = OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder().build())
            .build();
        tracer = new OtelTracer(otel.getTracer("test"), new OtelCurrentTraceContext(), event -> {});
    }

    @After
    public void tearDown() throws IOException {
        otel.close();
        trino.shutdown();
    }

    private TrinoHttpClient trinoHttpClient() {
        return new TrinoHttpClient(tracer, new OkHttpClient(), trino.url("/").toString(),
            new ServiceAccountAuthenticator(), tenantContextAccessor);
    }

    @Test
    public void cancelQuery_should_reject_when_pageIsAnAbsoluteUrl() {
        String absolutePage = trino.url(PAGE_PATH).toString();

        assertThatThrownBy(() -> trinoHttpClient().cancelQuery(absolutePage, Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining(absolutePage);

        assertThat(trino.getRequestCount()).isZero();
    }

    @Test
    public void cancelQuery_should_addressAPageGivenAsARelativePath() throws Exception {
        trino.enqueue(new MockResponse().setResponseCode(204));

        int status = trinoHttpClient().cancelQuery(PAGE_PATH.substring(1), Map.of());

        RecordedRequest recorded = trino.takeRequest();
        assertThat(requestLine(recorded)).as("HTTP request to trino").isEqualTo("DELETE " + PAGE_PATH);
        assertThat(status).as("HTTP response code from data-connect-trino back to caller").isEqualTo(204);
    }

    @Test
    public void cancelQuery_should_passThroughTrinosResponseStatus() {
        trino.enqueue(new MockResponse().setResponseCode(404));

        int status = trinoHttpClient().cancelQuery(PAGE_PATH.substring(1), Map.of());

        assertThat(status).as("the status reported for a page Trino does not recognize").isEqualTo(404);
    }

    private static String requestLine(RecordedRequest request) {
        return request.getMethod() + " " + request.getPath();
    }
}
