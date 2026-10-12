package com.parcel.marketanalysis.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(MarketAnalysisProperties.class)
public class ClientConfiguration {
    @Bean
    RestClient datasetRestClient(RestClient.Builder builder, MarketAnalysisProperties properties) {
        return client(builder, properties.datasetApiUrl().toString(), properties);
    }

    @Bean
    RestClient estimatorRestClient(RestClient.Builder builder, MarketAnalysisProperties properties) {
        return client(builder, properties.estimatorApiUrl().toString(), properties);
    }

    private RestClient client(RestClient.Builder builder, String baseUrl, MarketAnalysisProperties properties) {
        var requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder.clone().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Bean
    CacheManager cacheManager(MarketAnalysisProperties properties) {
        var manager = new CaffeineCacheManager("housingDataset");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(properties.cacheTtl().toMillis(), TimeUnit.MILLISECONDS)
                .maximumSize(1));
        return manager;
    }
}