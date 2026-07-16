package com.travel.gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验 Gateway 路由熔断触发时，客户端能收到规范的 JSON 响应体。
 * 使用 {@link WebTestClient#bindToController} 直接绑定控制器，
 * 不拉起完整 Spring 上下文，以便在无 Redis/Nacos 的环境中也能运行。
 */
class FallbackControllerTest {

    private final WebTestClient client = WebTestClient.bindToController(new FallbackController()).build();

    @Test
    void business_fallback_should_return_503_envelope() {
        client.get().uri("/fallback/business")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo(503)
                .jsonPath("$.service").isEqualTo("business-service")
                .jsonPath("$.message").value(v -> assertThat(v).isNotNull());
    }

    @Test
    void ai_fallback_should_identify_service() {
        client.get().uri("/fallback/ai")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.service").isEqualTo("ai-service");
    }

    @Test
    void prediction_fallback_should_identify_service() {
        client.get().uri("/fallback/prediction")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.service").isEqualTo("prediction-service");
    }
}
