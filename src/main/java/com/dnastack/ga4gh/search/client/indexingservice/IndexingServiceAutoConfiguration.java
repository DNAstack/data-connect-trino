package com.dnastack.ga4gh.search.client.indexingservice;

import com.dnastack.ga4gh.search.DataModelSupplier;
import com.dnastack.oauth.client.OAuthClientFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class IndexingServiceAutoConfiguration {
    @Bean("indexingServiceClient")
    @ConditionalOnProperty(name = {"app.indexing-service.enabled"}, havingValue = "true")
    public IndexingServiceClient indexingServiceClient(OAuthClientFactory factory, IndexingServiceConfiguration configuration) {
        log.info("Initializing the indexing service API client...");
        return factory.builderWithCommonSetup(configuration.getOauthClient())
            .target(IndexingServiceClient.class, configuration.getBaseUri());
    }

    @Bean
    @ConditionalOnBean(IndexingServiceClient.class)
    public DataModelSupplier indexingServiceDataModelSupplier(IndexingServiceClient client) {
        log.info("Initializing the data model supplier with the indexing service API client...");
        return new IndexingServiceDataModelSupplier(client);
    }

}
