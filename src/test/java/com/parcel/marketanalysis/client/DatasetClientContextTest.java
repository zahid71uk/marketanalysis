package com.parcel.marketanalysis.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.parcel.marketanalysis.config.MarketAnalysisProperties;
import com.parcel.marketanalysis.service.MarketAnalysisService;
import com.parcel.marketanalysis.web.MarketFilter;
import com.parcel.marketanalysis.web.MarketAnalysisController;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DatasetClientContextTest {
    private static final String TOKEN = "dataset-cache-test-token";
    private static final AtomicInteger REQUEST_COUNT = new AtomicInteger();
    private static final AtomicReference<Integer> PRICE = new AtomicReference<>(250_000);
    private static final AtomicBoolean MODEL_LOADED = new AtomicBoolean(true);
    private static final HttpServer SERVER = startServer();

    @Autowired
    private DatasetClient datasetClient;

    @Autowired
    private MarketAnalysisService marketAnalysisService;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configureDataset(DynamicPropertyRegistry registry) {
        registry.add("market-analysis.dataset-api-url",
                () -> "http://127.0.0.1:" + SERVER.getAddress().getPort());
        registry.add("market-analysis.cache-invalidation-token", () -> TOKEN);
    }

    @BeforeEach
    void resetDataset() {
        datasetClient.evictHousesCache();
        REQUEST_COUNT.set(0);
        PRICE.set(250_000);
        MODEL_LOADED.set(true);
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @Test
    void authenticatedInvalidationMakesNextReadFetchUpdatedDataset() {
        assertThat(datasetClient.getHouses().getFirst().price()).isEqualTo(250_000);
        assertThat(datasetClient.getHouses().getFirst().price()).isEqualTo(250_000);
        assertThat(REQUEST_COUNT).hasValue(1);

        PRICE.set(375_000);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Dataset-Cache-Token", TOKEN);
        var response = restTemplate.exchange(
                "/api/v1/market-analysis/internal/dataset-cache",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(datasetClient.getHouses().getFirst().price()).isEqualTo(375_000);
        assertThat(REQUEST_COUNT).hasValue(2);
    }

    @Test
    void unavailableModelHidesAndEvictsPreviouslyCachedRows() {
        var filter = new MarketFilter(null, null, null, null, null, null);
        assertThat(marketAnalysisService.getSummary(filter).sampleSize()).isEqualTo(1);
        assertThat(REQUEST_COUNT).hasValue(1);

        MODEL_LOADED.set(false);
        assertThat(marketAnalysisService.getSummary(filter).sampleSize()).isZero();

        MODEL_LOADED.set(true);
        PRICE.set(375_000);
        assertThat(marketAnalysisService.getSummary(filter).meanPrice()).isEqualTo(375_000);
        assertThat(REQUEST_COUNT).hasValue(2);
    }

    @Test
    void missingOrIncorrectTokenDoesNotEvictCache() {
        assertThat(datasetClient.getHouses().getFirst().price()).isEqualTo(250_000);
        PRICE.set(375_000);

        var missingTokenResponse = restTemplate.exchange(
                "/api/v1/market-analysis/internal/dataset-cache",
                HttpMethod.POST,
                HttpEntity.EMPTY,
                Void.class);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Dataset-Cache-Token", "wrong-token");
        var incorrectTokenResponse = restTemplate.exchange(
                "/api/v1/market-analysis/internal/dataset-cache",
                HttpMethod.POST,
                new HttpEntity<>(headers),
                Void.class);

        assertThat(missingTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(incorrectTokenResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(datasetClient.getHouses().getFirst().price()).isEqualTo(250_000);
        assertThat(REQUEST_COUNT).hasValue(1);
    }

    @Test
    void invalidationIsUnavailableWhenServerTokenIsNotConfigured() {
        MarketAnalysisService service = mock(MarketAnalysisService.class);
        MarketAnalysisProperties properties = new MarketAnalysisProperties(
                URI.create("http://dataset.test"),
                URI.create("http://estimator.test"),
                Duration.ofMinutes(5),
                Duration.ofSeconds(2),
                Duration.ofSeconds(8),
                "");
        MarketAnalysisController controller = new MarketAnalysisController(service, properties);

        assertThatThrownBy(() -> controller.invalidateDatasetCache(TOKEN))
                .isInstanceOfSatisfying(ResponseStatusException.class, error ->
                        assertThat(error.getStatusCode()).isEqualTo(HttpStatusCode.valueOf(503)));
        verifyNoInteractions(service);
    }

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/health", exchange -> {
                byte[] response = ("{\"status\":\"ok\",\"model_loaded\":" + MODEL_LOADED.get() + "}")
                        .getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.createContext("/api/v1/dataset/houses", exchange -> {
                REQUEST_COUNT.incrementAndGet();
                String body = """
                        {"houses":[{"id":1,"squareFootage":1550,"bedrooms":3,"bathrooms":2,"yearBuilt":1997,"lotSize":6800,"distanceToCityCenter":4.1,"schoolRating":7.6,"price":%d}],"count":1}
                        """.formatted(PRICE.get());
                byte[] response = body.getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}