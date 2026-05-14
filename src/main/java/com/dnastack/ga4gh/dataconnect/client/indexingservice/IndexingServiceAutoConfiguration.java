package com.dnastack.ga4gh.dataconnect.client.indexingservice;

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
public class IndexingServiceAutoConfiguration {
    @Bean("indexingServiceClient")
    @ConditionalOnProperty(name = {"app.indexing-service.enabled"}, havingValue = "true")
    public IndexingServiceClient indexingServiceClient(
        OAuthClientFactoryConfiguration oAuthClientFactoryConfiguration,
        IndexingServiceConfiguration configuration,
        ObservationRegistry observationRegistry
    ) {
        log.info("Initializing the indexing service API client...");
        OkHttpClient httpClient = OkHttpClients.buildOkHttpClient(
            "indexing-service", observationRegistry, IndexingServiceClient.class);
        feign.okhttp.OkHttpClient feignClient = new feign.okhttp.OkHttpClient(httpClient);
        return FeignClients.newBuilder(
                oAuthClientFactoryConfiguration.getDefaultConfig().withOverrides(configuration.getOauthClient()),
                feignClient,
                feignClient)
            .target(IndexingServiceClient.class, configuration.getBaseUri());
    }

    @Bean
    @ConditionalOnBean(IndexingServiceClient.class)
    public DataModelSupplier indexingServiceDataModelSupplier(IndexingServiceClient client,IndexingServiceConfiguration configuration) {
        log.info("Initializing the data model supplier with the indexing service API client...");
        return new IndexingServiceDataModelSupplier(client,configuration.getPublisherCatalogName());
    }

}
