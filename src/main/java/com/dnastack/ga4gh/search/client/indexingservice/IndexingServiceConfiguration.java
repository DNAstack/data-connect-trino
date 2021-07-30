package com.dnastack.ga4gh.search.client.indexingservice;

import com.dnastack.oauth.client.OAuthClientConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.indexing-service")
public class IndexingServiceConfiguration {
    private boolean enabled = false;
    private String baseUri;
    private OAuthClientConfiguration oauthClient;
}
