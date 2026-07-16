package com.travel.ai.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class PredictionClientTest {

    private static final String BASE_URL = "http://localhost:18001";

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private PredictionClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new PredictionClient(restTemplate);
        ReflectionTestUtils.setField(client, "predictionUrl", BASE_URL);
        ReflectionTestUtils.setField(client, "defaultModel", "dual_stream");
    }

    @Test
    void isAvailable_should_hit_health_endpoint() {
        server.expect(requestTo(BASE_URL + "/health"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"UP\"}", MediaType.APPLICATION_JSON));

        assertTrue(client.isAvailable());
        server.verify();
    }

    @Test
    void getPrediction_should_call_flow_endpoint_with_default_model() {
        String body = "{\"scenicId\":1,\"predictions\":[{\"date\":\"2026-04-20\",\"expectedFlow\":1200,\"congestionLevel\":\"low\"}]}";
        server.expect(requestTo(BASE_URL + "/api/prediction/flow/1?days=3&model=dual_stream"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<Map<String, Object>> result = client.getPrediction(1L, 3);

        assertEquals(1, result.size());
        assertEquals(1200, result.get(0).get("expectedFlow"));
        server.verify();
    }

    @Test
    void getPrediction_should_allow_explicit_model() {
        String body = "{\"predictions\":[{\"date\":\"2026-04-20\",\"expectedFlow\":900}]}";
        server.expect(requestTo(BASE_URL + "/api/prediction/flow/2?days=1&model=arima"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<Map<String, Object>> result = client.getPrediction(2L, 1, "arima");
        assertEquals(1, result.size());
        server.verify();
    }

    @Test
    void getTotalPrediction_should_parse_growth_and_accuracy() {
        String body = "{\"predictions\":[],\"growthRate\":5.1,\"accuracy\":92.0}";
        server.expect(requestTo(BASE_URL + "/api/prediction/total?days=7&model=dual_stream"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        Map<String, Object> total = client.getTotalPrediction(7);
        assertNotNull(total);
        assertEquals(5.1, ((Number) total.get("growthRate")).doubleValue(), 1e-6);
        server.verify();
    }

    @Test
    void getHourlyPrediction_should_read_hourlyData_field() {
        String body = "{\"hourlyData\":[{\"hour\":9,\"expectedFlow\":120,\"congestionLevel\":\"舒适\"}]}";
        server.expect(requestTo(BASE_URL + "/api/prediction/hourly/1?date=2026-04-20&model=dual_stream"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        List<Map<String, Object>> hourly = client.getHourlyPrediction(1L, "2026-04-20", null);
        assertEquals(1, hourly.size());
        assertEquals(9, hourly.get(0).get("hour"));
        server.verify();
    }

    @Test
    void invalid_arguments_should_return_empty_without_remote_call() {
        assertTrue(client.getPrediction(null, 3).isEmpty());
        assertTrue(client.getPrediction(1L, 0).isEmpty());
        assertTrue(client.getHourlyPrediction(1L, "", null).isEmpty());
    }
}
