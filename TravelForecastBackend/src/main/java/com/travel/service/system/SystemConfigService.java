package com.travel.service.system;

import java.util.Map;

public interface SystemConfigService {
    Map<String, String> getAllConfigs();
    String getConfig(String key);
    void setConfig(String key, String value);
    void setConfig(String key, String value, String description);
    void deleteConfig(String key);
}

