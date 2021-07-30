package com.dnastack.ga4gh.search.client.tablesregistry;

import com.dnastack.ga4gh.search.client.common.SimpleLogger;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import feign.Feign;
import feign.Logger;
import feign.codec.Encoder;
import feign.form.FormEncoder;
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

import java.util.Optional;

@Slf4j
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.tables-registry.auth")
@Deprecated(since = "2021-06-01 per #177369206")
public class OAuthClientConfig {
    /**
     * The URI to use for authenticated service to service communication. This is expected to be an OIDC compliant token
     * endpoint which will accept {@code client_credentials}
     */
    String authenticationUri;

    /**
     * The service account client id which will be used to authenticate this service against others
     */
    String clientId;

    /**
     * The service account client secret which will be used to authenticate this service against others
     */
    String clientSecret;

    String audience;

    @Autowired
    SimpleLogger simpleLogger;

    Encoder encoder;

    private ObjectMapper mapper = new ObjectMapper()
        .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Bean
    @ConditionalOnProperty(name = "app.tables-registry.auth.authentication-uri")
    public OAuthClient oAuthClient() {
        return Optional
            .ofNullable(authenticationUri)
            .flatMap(authenticationUri -> Optional.of(
                Feign.builder()
                    .client(new OkHttpClient())
                    .encoder(new FormEncoder(new JacksonEncoder(mapper)))
                    .decoder(new JacksonDecoder(mapper))
                    .logger(simpleLogger)
                    .logLevel(Logger.Level.BASIC)
                    .target(OAuthClient.class, authenticationUri)
                )
            )
            .orElse(client -> {
                log.warn("The client for token exchange is not defined.");
                return null;
            });
    }
}
