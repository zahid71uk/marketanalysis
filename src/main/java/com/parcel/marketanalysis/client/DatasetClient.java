package com.parcel.marketanalysis.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.parcel.marketanalysis.exception.UpstreamUnavailableException;
import com.parcel.marketanalysis.model.DatasetResponse;
import com.parcel.marketanalysis.model.HousingRecord;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class DatasetClient {
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
        } catch (RestClientException exception) {
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
        } catch (RestClientException | IllegalStateException exception) {
            throw new UpstreamUnavailableException("Dataset service", exception);
        }
    }

    @CacheEvict(cacheNames = "housingDataset", allEntries = true)
    public void evictHousesCache() {
    }

    private record ModelHealthResponse(@JsonProperty("model_loaded") boolean modelLoaded) {
    }
}