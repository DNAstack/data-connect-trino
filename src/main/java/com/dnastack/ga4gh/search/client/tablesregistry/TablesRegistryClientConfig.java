package com.dnastack.ga4gh.search.client.tablesregistry;

import com.dnastack.ga4gh.search.client.common.SimpleLogger;
import com.dnastack.ga4gh.search.client.tablesregistry.model.AccessToken;
import com.dnastack.ga4gh.search.client.tablesregistry.model.OAuthRequest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import feign.Feign;
import feign.Logger;
import feign.RequestInterceptor;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties("app.tables-registry")
@Deprecated(since = "2021-06-01 per #177369206")
public class TablesRegistryClientConfig {

    private Boolean skip;
    private String url;

    @Autowired
    private SimpleLogger simpleLogger;

    @Autowired (required = false)
    private OAuthClient oAuthClient;

    @Autowired
    private OAuthClientConfig oAuthClientConfig;

    private ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private OAuthRequest getOAuthRequest() {
        OAuthRequest oAuthRequest = new OAuthRequest();
        oAuthRequest.setClientId(oAuthClientConfig.getClientId());
        oAuthRequest.setClientSecret(oAuthClientConfig.getClientSecret());
        oAuthRequest.setGrantType("client_credentials");
        oAuthRequest.setAudience(oAuthClientConfig.getAudience());
        return oAuthRequest;
    }

    private RequestInterceptor getRequestInterceptor() {
        return (template) -> {
            AccessToken accessToken = oAuthClient.getToken(getOAuthRequest());
            template.header("Authorization", "Bearer " + accessToken.getToken());
        };
    }

    @Bean
    @ConditionalOnProperty(name = "app.tables-registry.url")
    public TablesRegistryClient tablesRegistryClient() {
        // TODO Refactor with OAuthClientFactory
        if (url == null) {
            log.warn("The client for Table Registry is not defined.");
            return null;
        }
        return Feign.builder()
                    .client(new OkHttpClient())
                    .encoder(new JacksonEncoder(mapper))
                    .decoder(new JacksonDecoder(mapper))
                    .logger(simpleLogger)
                    .logLevel(Logger.Level.BASIC)
                    .requestInterceptor(getRequestInterceptor())
                    .target(TablesRegistryClient.class, url);

    }

    @Bean
    @ConditionalOnProperty(name = "app.tables-registry.url")
    public TablesRegistryDataModelSupplier tablesRegistryDataModelSupplier(TablesRegistryClient client, OAuthClientConfig config) {
        return new TablesRegistryDataModelSupplier(client, config);
    }
}
