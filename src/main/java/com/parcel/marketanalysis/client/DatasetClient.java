package com.parcel.marketanalysis.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.parcel.marketanalysis.exception.UpstreamUnavailableException;
import com.parcel.marketanalysis.model.DatasetResponse;
import com.parcel.marketanalysis.model.HousingRecord;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class DatasetClient {
    private static final Logger logger = LoggerFactory.getLogger(DatasetClient.class);
    private final RestClient client;

    public DatasetClient(RestClient datasetRestClient) {
        this.client = datasetRestClient;
    }

    public boolean isModelLoaded() {
        try {
            ModelHealthResponse response = client.get()
                    .uri("/health")
                    .retrieve()
                    .body(ModelHealthResponse.class);
            return response != null && response.modelLoaded();
        } catch (RestClientResponseException exception) {
            logger.warn("Dataset service health check returned HTTP {}", exception.getStatusCode().value());
            throw new UpstreamUnavailableException("Dataset service", exception);
        } catch (RestClientException exception) {
            logger.warn("Dataset service health check failed ({})", exception.getClass().getSimpleName());
            throw new UpstreamUnavailableException("Dataset service", exception);
        }
    }

    @Cacheable("housingDataset")
    public List<HousingRecord> getHouses() {
        try {
            DatasetResponse response = client.get()
                    .uri("/api/v1/dataset/houses")
                    .retrieve()
                    .body(DatasetResponse.class);
            if (response == null || response.houses() == null || response.count() != response.houses().size()) {
                throw new IllegalStateException("The dataset service returned an invalid response.");
            }
            return List.copyOf(response.houses());
        } catch (RestClientResponseException exception) {
            logger.warn("Dataset service returned HTTP {} for source rows", exception.getStatusCode().value());
            throw new UpstreamUnavailableException("Dataset service", exception);
        } catch (RestClientException exception) {
            logger.warn("Dataset service request failed ({})", exception.getClass().getSimpleName());
            throw new UpstreamUnavailableException("Dataset service", exception);
        } catch (IllegalStateException exception) {
            logger.warn("Dataset service returned an invalid source-row response");
            throw new UpstreamUnavailableException("Dataset service", exception);
        }
    }

    @CacheEvict(cacheNames = "housingDataset", allEntries = true)
    public void evictHousesCache() {
    }

    private record ModelHealthResponse(@JsonProperty("model_loaded") boolean modelLoaded) {
    }
}