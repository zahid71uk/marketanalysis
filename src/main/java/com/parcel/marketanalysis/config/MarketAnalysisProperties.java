package com.parcel.marketanalysis.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "market-analysis")
public record MarketAnalysisProperties(
	URI datasetApiUrl,
	URI estimatorApiUrl,
	Duration cacheTtl,
	Duration connectTimeout,
	Duration readTimeout,
	String cacheInvalidationToken) {
}