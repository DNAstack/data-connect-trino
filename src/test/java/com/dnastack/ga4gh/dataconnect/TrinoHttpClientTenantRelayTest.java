package com.dnastack.ga4gh.dataconnect;

import com.dnastack.ga4gh.dataconnect.adapter.security.ServiceAccountAuthenticator;
import com.dnastack.ga4gh.dataconnect.adapter.trino.TrinoHttpClient;
import com.dnastack.tenancy.context.TenantContextAccessor;
import com.dnastack.tenancy.context.TenantId;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies how the request's tenant reaches Trino: as an extra credential of its own, which the Trino plugins scope
 * their authorization decisions to. It is sent separately from the caller's {@code userToken} because an anonymous
 * request supplies no token at all and its policy evaluation still has to be tenant-scoped.
 */
public class TrinoHttpClientTenantRelayTest {

    private static final String QUERY_RESPONSE =
        "{\"id\":\"20260903_000000_00000_aaaaa\",\"columns\":[],\"data\":[],\"stats\":{\"state\":\"FINISHED\"}}";

    private MockWebServer trino;
    private Tracer tracer;
    private final TenantContextAccessor tenantContextAccessor = new TenantContextAccessor();

    @Before
    public void setUp() throws IOException {
        trino = new MockWebServer();
        trino.start();
        trino.enqueue(new MockResponse().setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(QUERY_RESPONSE));

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
    public void trinoQuery_should_carryTheRequestTenantInAnExtraCredential() throws Exception {
        UUID tenantId = UUID.randomUUID();

        tenantContextAccessor.runAs(tenantId, () -> trinoHttpClient().query("SELECT 1", Map.of()));

        RecordedRequest recorded = trino.takeRequest();
        assertThat(recorded.getHeaders().values("X-Trino-Extra-Credential"))
            .as("the extra credentials of a query run within a tenant")
            .contains("tenantId=" + tenantId);
    }

    @Test
    public void trinoQuery_should_carryTheManagementTenant_when_theRequestNamedNoTenant() throws Exception {
        trinoHttpClient().query("SELECT 1", Map.of());

        RecordedRequest recorded = trino.takeRequest();
        assertThat(recorded.getHeaders().values("X-Trino-Extra-Credential"))
            .as("the extra credentials of a query outside any tenant")
            .contains("tenantId=" + TenantId.MANAGEMENT.asString());
    }

    @Test
    public void trinoQuery_should_replaceATenantTheCallerAsserted() throws Exception {
        UUID requestTenant = UUID.randomUUID();
        UUID assertedTenant = UUID.randomUUID();

        // Extra credentials reach us from the caller through the GA4GH-Search-Authorization header, so a caller can
        // put anything in one — including a tenant that is not the one it was authorized for.
        tenantContextAccessor.runAs(requestTenant,
            () -> trinoHttpClient().query("SELECT 1", Map.of("tenantId", assertedTenant.toString())));

        RecordedRequest recorded = trino.takeRequest();
        assertThat(recorded.getHeaders().values("X-Trino-Extra-Credential"))
            .as("the extra credentials of a query whose caller asserted a tenant of its own")
            .contains("tenantId=" + requestTenant)
            .doesNotContain("tenantId=" + assertedTenant);
    }

    @Test
    public void trinoQuery_should_carryTheCallersOtherExtraCredentials() throws Exception {
        trinoHttpClient().query("SELECT 1", Map.of("userToken", "a-user-token"));

        RecordedRequest recorded = trino.takeRequest();
        assertThat(recorded.getHeaders().values("X-Trino-Extra-Credential"))
            .as("the extra credentials of a query carrying a user token")
            .contains("userToken=a-user-token");
    }
}
