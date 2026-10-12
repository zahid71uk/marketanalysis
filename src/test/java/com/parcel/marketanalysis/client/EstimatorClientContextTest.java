package com.parcel.marketanalysis.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.parcel.marketanalysis.model.EstimateRequest;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class EstimatorClientContextTest {
    private static final AtomicReference<String> RECEIVED_BODY = new AtomicReference<>();
    private static final HttpServer SERVER = startServer();

    @Autowired
    private EstimatorClient estimatorClient;

    @DynamicPropertySource
    static void configureEstimator(DynamicPropertyRegistry registry) {
        registry.add("market-analysis.estimator-api-url",
                () -> "http://127.0.0.1:" + SERVER.getAddress().getPort());
    }

    @AfterAll
    static void stopServer() {
        SERVER.stop(0);
    }

    @Test
    void productionConfiguredClientSendsJsonEntity() {
        var response = estimatorClient.estimate(new EstimateRequest(1550, 3, 2, 1997, 6800, 4.1, 7.6));

        assertThat(response.predictedPrice()).isEqualTo(258061.4);
        assertThat(RECEIVED_BODY.get())
                .contains("\"square_footage\":1550")
                .contains("\"school_rating\":7.6");
    }

    private static HttpServer startServer() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/estimates", exchange -> {
                RECEIVED_BODY.set(new String(exchange.getRequestBody().readAllBytes()));
                byte[] body = "{\"predicted_price\":258061.4}".getBytes();
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }
}