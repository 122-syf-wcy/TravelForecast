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
 * Python 预测服务客户端。
 * 与 TravelForecast-PythonPredictionService (FastAPI, 默认 8001) 对接，
 * 路由约定见 main.py: /api/prediction/flow/{scenic_id}、/api/prediction/total 等。
 */
@Component
public class PredictionClient {

    private static final Logger log = LoggerFactory.getLogger(PredictionClient.class);

    /** 默认预测模型，可在 application.yml 通过 prediction-service.default-model 覆盖 */
    private static final String DEFAULT_MODEL = "dual_stream";

    private final RestTemplate restTemplate;

    @Value("${prediction-service.url:http://localhost:8001}")
    private String predictionUrl;

    @Value("${prediction-service.default-model:dual_stream}")
    private String defaultModel;

    public PredictionClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

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
     * 获取景区未来 N 天客流预测。
     * 返回列表每一项为驼峰字段：date, weekday, expectedFlow, peakHours,
     * weatherCondition, temperature, congestionLevel, components?
     */
    public List<Map<String, Object>> getPrediction(Long scenicId, int days) {
        return getPrediction(scenicId, days, null);
    }

    /**
     * 带模型类型的预测调用，model 取值：arima / lstm / dual_stream。
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getPrediction(Long scenicId, int days, String model) {
        if (scenicId == null || days <= 0) {
            return Collections.emptyList();
        }
        String effectiveModel = (model == null || model.isEmpty())
                ? (defaultModel == null || defaultModel.isEmpty() ? DEFAULT_MODEL : defaultModel)
                : model;
        try {
            String url = String.format("%s/api/prediction/flow/%d?days=%d&model=%s",
                    predictionUrl, scenicId, days, effectiveModel);

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
     * 获取六盘水全域聚合预测。
     * 返回 Map 中关键字段：predictions、growthRate、accuracy、confidence。
     */
    public Map<String, Object> getTotalPrediction(int days) {
        return getTotalPrediction(days, null);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getTotalPrediction(int days, String model) {
        String effectiveModel = (model == null || model.isEmpty())
                ? (defaultModel == null || defaultModel.isEmpty() ? DEFAULT_MODEL : defaultModel)
                : model;
        try {
            String url = String.format("%s/api/prediction/total?days=%d&model=%s",
                    predictionUrl, days, effectiveModel);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return JSON.parseObject(response.getBody(), Map.class);
            }
            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("调用全域聚合预测失败: error={}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 获取指定日期的小时级预测（数组形式）。
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getHourlyPrediction(Long scenicId, String date, String model) {
        if (scenicId == null || date == null || date.isEmpty()) {
            return Collections.emptyList();
        }
        String effectiveModel = (model == null || model.isEmpty())
                ? (defaultModel == null || defaultModel.isEmpty() ? DEFAULT_MODEL : defaultModel)
                : model;
        try {
            String url = String.format("%s/api/prediction/hourly/%d?date=%s&model=%s",
                    predictionUrl, scenicId, date, effectiveModel);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject json = JSON.parseObject(response.getBody());
                JSONArray data = json.getJSONArray("hourlyData");
                if (data != null) {
                    return (List<Map<String, Object>>) (List<?>) data.toJavaList(Map.class);
                }
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("调用小时级预测失败: scenicId={}, date={}, error={}", scenicId, date, e.getMessage());
            return Collections.emptyList();
        }
    }
}
