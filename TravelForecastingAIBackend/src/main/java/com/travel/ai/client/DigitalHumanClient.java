package com.travel.ai.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 数字人后端客户端。
 * 与 TravelForecast-DigitalHuman (FastAPI, 默认 8083) 对接，
 * 路由与响应结构见 backend/app/api/chat.py 与 backend/main.py。
 */
@Component
public class DigitalHumanClient {

    private static final Logger log = LoggerFactory.getLogger(DigitalHumanClient.class);

    private final RestTemplate restTemplate;

    @Value("${digital-human.url:http://localhost:8083}")
    private String digitalHumanUrl;

    @Value("${digital-human.enabled:true}")
    private boolean enabled;

    public DigitalHumanClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public boolean isAvailable() {
        if (!enabled) return false;
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(digitalHumanUrl + "/health", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("数字人服务不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 通过 REST 接口向数字人发送文本消息，返回 LLM 回复文本。
     * FastAPI 侧响应体：{"response": "...", "session_id": "..."}
     */
    public String chat(String message, String sessionId) {
        if (!enabled) {
            log.warn("数字人服务未启用");
            return null;
        }

        try {
            Map<String, Object> request = Map.of(
                    "message", message,
                    "session_id", sessionId != null ? sessionId : ""
            );

            ResponseEntity<String> response = restTemplate.postForEntity(
                    digitalHumanUrl + "/api/chat",
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject json = JSON.parseObject(response.getBody());
                // 当前契约字段为 response；保留 reply 做向下兼容
                String reply = json.getString("response");
                if (reply == null || reply.isEmpty()) {
                    reply = json.getString("reply");
                }
                return reply;
            }
            return null;

        } catch (Exception e) {
            log.error("调用数字人聊天失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取数字人TTS语音数据
     */
    public byte[] textToSpeech(String text, String voice) {
        if (!enabled) return null;

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("text", text);
            if (voice != null && !voice.isEmpty()) {
                requestBody.put("voice", voice);
            }

            ResponseEntity<String> response = restTemplate.postForEntity(
                    digitalHumanUrl + "/api/tts",
                    requestBody,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject json = JSON.parseObject(response.getBody());
                String audioBase64 = json.getString("audio");
                if (audioBase64 != null && !audioBase64.isEmpty()) {
                    return Base64.getDecoder().decode(audioBase64);
                }
            }
            return null;

        } catch (Exception e) {
            log.error("调用数字人TTS失败: {}", e.getMessage());
            return null;
        }
    }
}
