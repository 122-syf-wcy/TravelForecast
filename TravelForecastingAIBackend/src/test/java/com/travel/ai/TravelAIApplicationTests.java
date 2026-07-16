package com.travel.ai;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Full Spring 上下文冒烟测试：需要 MySQL / Redis 在位才能通过。
 * 本地 `mvn test` 默认跳过，上 CI 后通过 `-Dspring.boot.test.run=true`
 * 或 profile 再启用。
 */
@Disabled("需要真实 MySQL/Redis，请在集成测试流水线启用")
@SpringBootTest
class TravelAIApplicationTests {

    @Test
    void contextLoads() {
    }
}
