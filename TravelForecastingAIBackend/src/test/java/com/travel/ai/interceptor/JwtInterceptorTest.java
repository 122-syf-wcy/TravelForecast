package com.travel.ai.interceptor;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtInterceptorTest {

    private static final String SECRET = "unit-test-jwt-secret-must-be-long-enough-for-hs256-123456";

    private JwtInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new JwtInterceptor();
        ReflectionTestUtils.setField(interceptor, "jwtSecret", SECRET);
        ReflectionTestUtils.setField(interceptor, "tokenHeader", "Authorization");
        ReflectionTestUtils.setField(interceptor, "tokenPrefix", "Bearer");
    }

    @Test
    void options_request_should_pass_through() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("OPTIONS");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(200, response.getStatus());
    }

    @Test
    void missing_token_should_be_allowed_for_public_routes() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ai-api/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(200, response.getStatus());
    }

    @Test
    void valid_token_should_populate_user_attributes() throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("alice")
                .claim("userId", 42L)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key)
                .compact();

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ai-api/chat");
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(42L, request.getAttribute("userId"));
        assertEquals("alice", request.getAttribute("username"));
    }

    @Test
    void invalid_token_should_be_rejected_with_401() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ai-api/chat");
        request.addHeader("Authorization", "Bearer obviously.not.a.jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean passed = interceptor.preHandle(request, response, new Object());

        assertFalse(passed, "Token 无效时必须拒绝请求");
        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Token"));
    }
}
