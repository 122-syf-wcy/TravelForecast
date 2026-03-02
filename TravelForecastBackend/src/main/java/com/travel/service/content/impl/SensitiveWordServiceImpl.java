package com.travel.service.content.impl;

import com.travel.entity.content.SensitiveWord;
import com.travel.mapper.content.SensitiveWordMapper;
import com.travel.service.content.SensitiveWordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 敏感词过滤服务实现
 */
@Slf4j
@Service
public class SensitiveWordServiceImpl implements SensitiveWordService, InitializingBean {

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    // 缓存敏感词及其处理方式
    private Map<String, SensitiveWord> sensitiveWordsCache = new HashMap<>();
    
    // 编译后的正则表达式缓存
    private Pattern sensitivePattern;
    
    /**
     * 初始化时加载敏感词（Spring容器初始化后调用）
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        refreshCache();
    }

    @Override
    public String filterSensitiveWords(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }

        if (sensitiveWordsCache.isEmpty()) {
            return text;
        }

        String result = text;
        
        // 使用正则表达式替换所有敏感词
        if (sensitivePattern != null) {
            result = sensitivePattern.matcher(text).replaceAll(matchResult -> {
                String word = matchResult.group();
                SensitiveWord sensitiveWord = sensitiveWordsCache.get(word.toLowerCase());
                if (sensitiveWord != null && "replace".equals(sensitiveWord.getAction())) {
                    return sensitiveWord.getReplacement() != null ? 
                           sensitiveWord.getReplacement() : "***";
                }
                return word;
            });
        }
        
        if (!result.equals(text)) {
            log.info("过滤敏感词：原文长度={}, 过滤后长度={}", text.length(), result.length());
        }
        
        return result;
    }

    @Override
    public boolean containsBlockedWords(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }

        if (sensitiveWordsCache.isEmpty()) {
            return false;
        }

        String lowerText = text.toLowerCase();
        
        // 检查是否包含需要拦截的敏感词
        for (Map.Entry<String, SensitiveWord> entry : sensitiveWordsCache.entrySet()) {
            SensitiveWord word = entry.getValue();
            if ("block".equals(word.getAction()) && lowerText.contains(entry.getKey())) {
                log.warn("检测到拦截级敏感词：{}", word.getWord());
                return true;
            }
        }
        
        return false;
    }

    @Override
    public void refreshCache() {
        try {
            List<SensitiveWord> words = sensitiveWordMapper.getAllActiveSensitiveWords();
            
            Map<String, SensitiveWord> newCache = new HashMap<>();
            StringBuilder regexBuilder = new StringBuilder();
            
            for (int i = 0; i < words.size(); i++) {
                SensitiveWord word = words.get(i);
                newCache.put(word.getWord().toLowerCase(), word);
                
                // 构建正则表达式（用于替换）
                if (i > 0) {
                    regexBuilder.append("|");
                }
                regexBuilder.append(Pattern.quote(word.getWord()));
            }
            
            sensitiveWordsCache = newCache;
            
            // 编译正则表达式（不区分大小写）
            if (regexBuilder.length() > 0) {
                sensitivePattern = Pattern.compile(regexBuilder.toString(), Pattern.CASE_INSENSITIVE);
            }
            
            log.info("敏感词缓存刷新成功，共加载{}个敏感词", words.size());
        } catch (Exception e) {
            log.error("刷新敏感词缓存失败", e);
        }
    }
}

