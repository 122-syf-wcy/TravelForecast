package com.travel.service.content;

/**
 * 敏感词过滤服务
 */
public interface SensitiveWordService {
    
    /**
     * 过滤敏感词
     * @param text 原始文本
     * @return 过滤后的文本
     */
    String filterSensitiveWords(String text);
    
    /**
     * 检查文本是否包含需要拦截的敏感词
     * @param text 原始文本
     * @return true-包含拦截级敏感词，false-不包含
     */
    boolean containsBlockedWords(String text);
    
    /**
     * 刷新敏感词缓存
     */
    void refreshCache();
}

