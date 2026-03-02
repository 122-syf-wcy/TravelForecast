package com.travel.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.entity.system.SystemConfig;
import com.travel.mapper.system.SystemConfigMapper;
import com.travel.service.system.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigMapper configMapper;

    @Override
    public Map<String, String> getAllConfigs() {
        List<SystemConfig> configs = configMapper.selectList(null);
        Map<String, String> result = new HashMap<>();
        for (SystemConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }

    @Override
    public String getConfig(String key) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig config = configMapper.selectOne(wrapper);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    @Transactional
    public void setConfig(String key, String value) {
        setConfig(key, value, null);
    }

    @Override
    @Transactional
    public void setConfig(String key, String value, String description) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        SystemConfig existingConfig = configMapper.selectOne(wrapper);

        if (existingConfig != null) {
            existingConfig.setConfigValue(value);
            if (description != null) {
                existingConfig.setDescription(description);
            }
            existingConfig.setUpdatedAt(LocalDateTime.now());
            configMapper.updateById(existingConfig);
            log.info("更新配置: {} = {}", key, value);
        } else {
            SystemConfig newConfig = new SystemConfig();
            newConfig.setConfigKey(key);
            newConfig.setConfigValue(value);
            newConfig.setDescription(description);
            newConfig.setCreatedAt(LocalDateTime.now());
            newConfig.setUpdatedAt(LocalDateTime.now());
            configMapper.insert(newConfig);
            log.info("创建配置: {} = {}", key, value);
        }
    }

    @Override
    @Transactional
    public void deleteConfig(String key) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SystemConfig::getConfigKey, key);
        configMapper.delete(wrapper);
        log.info("删除配置: {}", key);
    }
}

