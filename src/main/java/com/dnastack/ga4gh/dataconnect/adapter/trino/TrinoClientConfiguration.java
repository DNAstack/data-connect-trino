package com.dnastack.ga4gh.dataconnect.adapter.trino;

import com.dnastack.ga4gh.dataconnect.adapter.security.ServiceAccountAuthenticator;
import com.dnastack.ga4gh.dataconnect.adapter.telemetry.TrinoTelemetryClient;
import com.dnastack.tenancy.context.TenantContextAccessor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.tracing.Tracer;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Assembles the client that talks to Trino. It lives beside the client rather than in the application-wide
 * configuration because the client needs the request tenant to relay it, and only the layers named in
 * {@code TenancyArchitectureTest} may hold the accessor that supplies it.
 */
@Configuration
public class TrinoClientConfiguration {

    private final String trinoDatasourceUrl;

    public TrinoClientConfiguration(@Value("${trino.datasource.url}") String trinoDatasourceUrl) {
        this.trinoDatasourceUrl = trinoDatasourceUrl;
    }

    @Bean
    public TrinoClient getTrinoClient(OkHttpClient httpClient,
                                      Tracer tracer,
                                      ServiceAccountAuthenticator accountAuthenticator,
                                      MeterRegistry registry,
                                      TenantContextAccessor tenantContextAccessor) {
        return new TrinoTelemetryClient(
            new TrinoHttpClient(tracer, httpClient, trinoDatasourceUrl, accountAuthenticator, tenantContextAccessor),
            registry);
    }
}
