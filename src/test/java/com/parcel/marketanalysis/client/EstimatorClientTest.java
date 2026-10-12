package com.parcel.marketanalysis.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.parcel.marketanalysis.model.EstimateRequest;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class EstimatorClientTest {
    @Test
    void sendsFeaturePayloadAsJsonToVersionedEstimateEndpoint() throws Exception {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://estimator.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
      var request = new EstimateRequest(1550, 3, 2, 1997, 6800, 4.1, 7.6);
      var objectMapper = new ObjectMapper();
      EstimatorClient client = new EstimatorClient(builder.build(), objectMapper);
        String expectedBody = """
                {
                  "square_footage": 1550,
                  "bedrooms": 3,
                  "bathrooms": 2,
                  "year_built": 1997,
                  "lot_size": 6800,
                  "distance_to_city_center": 4.1,
                  "school_rating": 7.6
                }
                """;
        server.expect(requestTo("http://estimator.test/api/v1/estimates"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", MediaType.APPLICATION_JSON_VALUE))
                .andExpect(content().json(expectedBody))
                .andRespond(withSuccess("{\"predicted_price\":258061.4}", MediaType.APPLICATION_JSON));

        var response = client.estimate(request);

        assertThat(response.predictedPrice()).isEqualTo(258061.4);
        server.verify();
    }

      @Test
      void sendsRequestBodyThroughApacheHttpTransport() throws Exception {
        AtomicReference<String> receivedBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/estimates", exchange -> {
          receivedBody.set(new String(exchange.getRequestBody().readAllBytes()));
          byte[] response = "{\"predicted_price\":258061.4}".getBytes();
          exchange.getResponseHeaders().set("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, response.length);
          exchange.getResponseBody().write(response);
          exchange.close();
        });
        server.start();

        try {
            var requestFactory = new HttpComponentsClientHttpRequestFactory();
          var client = RestClient.builder()
              .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
              .requestFactory(requestFactory)
              .build();

              new EstimatorClient(client, new ObjectMapper())
                .estimate(new EstimateRequest(1550, 3, 2, 1997, 6800, 4.1, 7.6));

          assertThat(receivedBody.get())
              .contains("\"square_footage\":1550")
              .contains("\"school_rating\":7.6");
        } finally {
          server.stop(0);
        }
      }
}