package com.parcel.marketanalysis.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parcel.marketanalysis.exception.UpstreamUnavailableException;
import com.parcel.marketanalysis.model.EstimateRequest;
import com.parcel.marketanalysis.model.EstimateResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class EstimatorClient {
    private static final Logger logger = LoggerFactory.getLogger(EstimatorClient.class);
    private final RestClient client;
    private final ObjectMapper objectMapper;

    public EstimatorClient(RestClient estimatorRestClient, ObjectMapper objectMapper) {
        this.client = estimatorRestClient;
        this.objectMapper = objectMapper;
    }

    public EstimateResponse estimate(EstimateRequest request) {
        byte[] requestBody;
        try {
            requestBody = objectMapper.writeValueAsBytes(request);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize the estimate request.", exception);
        }

        try {
            EstimateResponse response = client.post()
                    .uri("/api/v1/estimates")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(EstimateResponse.class);
            if (response == null || !Double.isFinite(response.predictedPrice())) {
                throw new IllegalStateException("The estimate service returned an invalid response.");
            }
            return response;
        } catch (RestClientResponseException exception) {
            logger.warn("Estimate service returned HTTP {}", exception.getStatusCode().value());
            throw new UpstreamUnavailableException("Estimate service", exception);
        } catch (RestClientException | IllegalStateException exception) {
            logger.warn("Estimate service request failed ({})", exception.getClass().getSimpleName());
            throw new UpstreamUnavailableException("Estimate service", exception);
        }
    }
}