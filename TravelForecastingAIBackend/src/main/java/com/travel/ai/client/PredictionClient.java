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
 * Python预测服务客户端
 * 调用Python FastAPI预测服务 (Port 8001)
 */
@Component
public class PredictionClient {

    private static final Logger log = LoggerFactory.getLogger(PredictionClient.class);

    private final RestTemplate restTemplate;

    @Value("${prediction-service.url:http://localhost:8001}")
    private String predictionUrl;

    public PredictionClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 检查预测服务是否可用
     */
    public boolean isAvailable() {
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(predictionUrl + "/health", String.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Python预测服务不可用: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取景区客流预测
     *
     * @param scenicId 景区ID
     * @param days     预测天数
     * @return 预测结果列表
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPrediction(Long scenicId, int days) {
        try {
            String url = String.format("%s/api/predict?scenic_id=%d&days=%d", predictionUrl, scenicId, days);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject json = JSON.parseObject(response.getBody());
                JSONArray predictions = json.getJSONArray("predictions");
                if (predictions != null) {
                    return (List<Map<String, Object>>) (List<?>) predictions.toJavaList(Map.class);
                }
            }
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("调用预测服务失败: scenicId={}, error={}", scenicId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取历史客流数据
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getHistoryData(Long scenicId, String startDate, String endDate) {
        try {
            String url = String.format("%s/api/history?scenic_id=%d&start_date=%s&end_date=%s",
                    predictionUrl, scenicId, startDate, endDate);

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject json = JSON.parseObject(response.getBody());
                JSONArray data = json.getJSONArray("data");
                if (data != null) {
                    return (List<Map<String, Object>>) (List<?>) data.toJavaList(Map.class);
                }
            }
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("获取历史数据失败: scenicId={}, error={}", scenicId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
