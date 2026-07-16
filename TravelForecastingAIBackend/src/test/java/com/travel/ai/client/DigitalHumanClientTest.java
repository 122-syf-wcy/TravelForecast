package com.travel.ai.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DigitalHumanClientTest {

    private static final String BASE_URL = "http://localhost:18083";

    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private DigitalHumanClient client;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);
        client = new DigitalHumanClient(restTemplate);
        ReflectionTestUtils.setField(client, "digitalHumanUrl", BASE_URL);
        ReflectionTestUtils.setField(client, "enabled", true);
    }

    @Test
    void chat_should_prefer_response_field_and_fallback_to_reply() {
        String body = "{\"response\":\"您好\",\"session_id\":\"abc\"}";
        server.expect(requestTo(BASE_URL + "/api/chat"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        String result = client.chat("你好", "abc");
        assertEquals("您好", result);
        server.verify();
    }

    @Test
    void chat_should_fall_back_to_reply_when_response_missing() {
        String body = "{\"reply\":\"（旧字段）\",\"session_id\":\"abc\"}";
        server.expect(requestTo(BASE_URL + "/api/chat"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        assertEquals("（旧字段）", client.chat("你好", "abc"));
        server.verify();
    }

    @Test
    void chat_should_return_null_when_disabled() {
        ReflectionTestUtils.setField(client, "enabled", false);
        assertNull(client.chat("你好", "abc"));
        // 没有任何真实请求被发送，server.verify() 不应报错
        server.verify();
    }
}
