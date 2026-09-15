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

/**
 * Verifies how a query cancellation addresses Trino. One method serves both callers that hold a page as this
 * service handed it out and callers that hold the absolute {@code next_page_url} the sweeps stored, so it has to
 * accept either form and reach the same place.
 */
public class TrinoHttpClientCancellationTest {

    private static final String PAGE_PATH = "/v1/statement/executing/20260903_000000_00000_aaaaa/slug/2";

    private MockWebServer trino;
    private Tracer tracer;
    private final TenantContextAccessor tenantContextAccessor = new TenantContextAccessor();

    @Before
    public void setUp() throws IOException {
        trino = new MockWebServer();
        trino.start();

        OpenTelemetrySdk otel = OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder().build())
            .build();
        tracer = new OtelTracer(otel.getTracer("test"), new OtelCurrentTraceContext(), event -> {});
    }

    @After
    public void tearDown() throws IOException {
        trino.shutdown();
    }

    private TrinoHttpClient trinoHttpClient() {
        return new TrinoHttpClient(tracer, new OkHttpClient(), trino.url("/").toString(),
            new ServiceAccountAuthenticator(), tenantContextAccessor);
    }

    @Test
    public void cancelQuery_should_addressAPageGivenAsAnAbsoluteUrl() throws Exception {
        trino.enqueue(new MockResponse().setResponseCode(204));

        // The sweeps cancel from the next_page_url they stored, which is the absolute URI Trino handed back.
        int status = trinoHttpClient().cancelQuery(trino.url(PAGE_PATH).toString(), Map.of());

        RecordedRequest recorded = trino.takeRequest();
        assertThat(recorded.getMethod()).as("the method of the cancellation").isEqualTo("DELETE");
        assertThat(recorded.getPath()).as("the path a cancellation by absolute URL reached").isEqualTo(PAGE_PATH);
        assertThat(status).as("the status reported for the cancellation").isEqualTo(204);
    }

    @Test
    public void cancelQuery_should_addressAPageGivenAsARelativePath() throws Exception {
        trino.enqueue(new MockResponse().setResponseCode(204));

        int status = trinoHttpClient().cancelQuery(PAGE_PATH.substring(1), Map.of());

        RecordedRequest recorded = trino.takeRequest();
        assertThat(recorded.getPath()).as("the path a cancellation by relative page reached").isEqualTo(PAGE_PATH);
        assertThat(status).as("the status reported for the cancellation").isEqualTo(204);
    }

    @Test
    public void cancelQuery_should_reportTheStatusTrinoAnswered() {
        trino.enqueue(new MockResponse().setResponseCode(404));

        int status = trinoHttpClient().cancelQuery(PAGE_PATH.substring(1), Map.of());

        assertThat(status).as("the status reported for a page Trino does not recognize").isEqualTo(404);
    }
}
