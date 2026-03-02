package com.travel.ai.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 业务后端服务客户端
 * 调用Java业务后端 (Port 8080)
 */
@Component
public class BusinessClient {

    private static final Logger log = LoggerFactory.getLogger(BusinessClient.class);

    private final RestTemplate restTemplate;

    @Value("${business-service.url:http://localhost:8080/api}")
    private String businessUrl;

    public BusinessClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 获取景区列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getScenicSpots() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    businessUrl + "/scenic-spots/list", String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject json = JSON.parseObject(response.getBody());
                JSONArray data = json.getJSONArray("data");
                if (data != null) {
                    return (List<Map<String, Object>>) (List<?>) data.toJavaList(Map.class);
                }
            }
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("获取景区列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取景区详情
     */
    public Map<String, Object> getScenicSpotDetail(Long scenicId) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    businessUrl + "/scenic-spots/" + scenicId, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject json = JSON.parseObject(response.getBody());
                return json.getJSONObject("data");
            }
            return null;

        } catch (Exception e) {
            log.error("获取景区详情失败: scenicId={}, error={}", scenicId, e.getMessage());
            return null;
        }
    }

    /**
     * 获取新闻公告列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getNews(int page, int size) {
        try {
            String url = String.format("%s/news?page=%d&size=%d", businessUrl, page, size);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject json = JSON.parseObject(response.getBody());
                JSONObject data = json.getJSONObject("data");
                if (data != null) {
                    JSONArray records = data.getJSONArray("records");
                    if (records != null) {
                        return (List<Map<String, Object>>) (List<?>) records.toJavaList(Map.class);
                    }
                }
            }
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("获取新闻列表失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取用户信息
     */
    public Map<String, Object> getUserInfo(Long userId) {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(
                    businessUrl + "/users/" + userId, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject json = JSON.parseObject(response.getBody());
                return json.getJSONObject("data");
            }
            return null;

        } catch (Exception e) {
            log.error("获取用户信息失败: userId={}, error={}", userId, e.getMessage());
            return null;
        }
    }
}
