package com.travel.service.prediction;

import java.util.List;

public interface AiService {
    String chat(String model, List<com.alibaba.dashscope.common.Message> messages, String apiKey) throws Exception;
}


