package com.dnastack.ga4gh.dataconnect.client.collectionservice;

import com.dnastack.oauth.client.OAuthClientConfiguration;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "app.collection-service")
public class CollectionServiceConfiguration {

    private boolean enabled = false;
    private String baseUri;
    private OAuthClientConfiguration oauthClient;
    private String collectionsCatalogName = null;

}
