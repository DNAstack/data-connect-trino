package com.dnastack.ga4gh.dataconnect.client.collectionservice;

import com.dnastack.ga4gh.dataconnect.DataModelSupplier;
import com.dnastack.oauth.client.starter.config.OAuthClientFactoryConfiguration;
import com.dnastack.oauth.feign.FeignClients;
import com.dnastack.oauth.okhttp.OkHttpClients;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CollectionServiceClientConfiguration {
    @Bean("collectionServiceClient")
    @ConditionalOnProperty(name = {"app.collection-service.enabled"}, havingValue = "true")
    public CollectionServiceClient collectionServiceClient(
        OAuthClientFactoryConfiguration oAuthClientFactoryConfiguration,
        CollectionServiceConfiguration configuration,
        ObservationRegistry observationRegistry
    ) {
        log.info("Initializing the collection-service API client...");
        OkHttpClient httpClient = OkHttpClients.buildOkHttpClient(
            "collection-service", observationRegistry, CollectionServiceClient.class);
        feign.okhttp.OkHttpClient feignClient = new feign.okhttp.OkHttpClient(httpClient);
        return FeignClients.newBuilder(
                oAuthClientFactoryConfiguration.getDefaultConfig().withOverrides(configuration.getOauthClient()),
                feignClient,
                feignClient)
            .target(CollectionServiceClient.class, configuration.getBaseUri());
    }

    @Bean
    @ConditionalOnBean(CollectionServiceClient.class)
    public DataModelSupplier collectionServiceDataModelSupplier(CollectionServiceClient client, CollectionServiceConfiguration configuration) {
        log.info("Initializing a collection-service data model supplier for catalog {}", configuration.getCollectionsCatalogName());
        return new CollectionServiceDataModelSupplier(client, configuration.getCollectionsCatalogName());
    }

}
