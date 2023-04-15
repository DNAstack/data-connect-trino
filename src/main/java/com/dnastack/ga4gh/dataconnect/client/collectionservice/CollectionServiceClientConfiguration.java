package com.dnastack.ga4gh.dataconnect.client.collectionservice;

import com.dnastack.ga4gh.dataconnect.DataModelSupplier;
import com.dnastack.oauth.client.OAuthClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class CollectionServiceClientConfiguration {
    @Bean("collectionServiceClient")
    @ConditionalOnProperty(name = {"app.collection-service.enabled"}, havingValue = "true")
    public CollectionServiceClient collectionServiceClient(OAuthClientFactory oAuthClientFactory, CollectionServiceConfiguration configuration) {
        log.info("Initializing the collection-service API client...");
        return oAuthClientFactory.builderWithCommonSetup(configuration.getOauthClient())
            .target(CollectionServiceClient.class, configuration.getBaseUri());
    }

    @Bean
    @ConditionalOnBean(CollectionServiceClient.class)
    public DataModelSupplier collectionServiceDataModelSupplier(CollectionServiceClient client, CollectionServiceConfiguration configuration) {
        log.info("Initializing a collection-service data model supplier for catalog {}", configuration.getCollectionsCatalogName());
        return new CollectionServiceDataModelSupplier(client, configuration.getCollectionsCatalogName());
    }

}
