package com.travel.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.ai.dto.KnowledgeRequest;
import com.travel.ai.entity.Knowledge;
import com.travel.ai.mapper.KnowledgeMapper;
import com.travel.ai.service.RagService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * RAG知识库检索服务实现
 * 基于关键词匹配的简易RAG实现
 *
 * 性能优化：
 * 1. 知识库搜索结果缓存1年（知识库数据几乎不变）
 * 2. 知识库列表缓存1年
 * 3. 增删改时自动清除相关缓存
 */
@Service
public class RagServiceImpl implements RagService {

    private static final Logger log = LoggerFactory.getLogger(RagServiceImpl.class);

    private static final String KNOWLEDGE_CACHE_PREFIX = "ai:knowledge:";
    private static final String KNOWLEDGE_LIST_CACHE = "ai:knowledge:list:";
    private static final String KNOWLEDGE_SEARCH_CACHE = "ai:knowledge:search:";

    @Autowired
    private KnowledgeMapper knowledgeMapper;

    @Autowired(required = false)
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${ai.knowledge.cache-ttl-hours:8760}")
    private long knowledgeCacheTtlHours;

    @Override
    @SuppressWarnings("unchecked")
    public List<Knowledge> search(KnowledgeRequest request) {
        // 缓存Key
        String cacheKey = KNOWLEDGE_SEARCH_CACHE + Math.abs(
                (request.getQuery() + ":" + request.getCategory() + ":" + request.getScenicId()).hashCode());

        // 尝试缓存
        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("知识库搜索命中缓存: {}", cacheKey);
                    return (List<Knowledge>) cached;
                }
            } catch (Exception e) {
                log.debug("读取知识库搜索缓存失败: {}", e.getMessage());
            }
        }

        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Knowledge::getEnabled, true);

        if (request.getCategory() != null && !request.getCategory().isEmpty()) {
            wrapper.eq(Knowledge::getCategory, request.getCategory());
        }

        if (request.getScenicId() != null) {
            wrapper.eq(Knowledge::getScenicId, request.getScenicId());
        }

        // 基于关键词匹配
        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            wrapper.and(w -> w
                    .like(Knowledge::getTitle, request.getQuery())
                    .or()
                    .like(Knowledge::getContent, request.getQuery())
                    .or()
                    .like(Knowledge::getKeywords, request.getQuery()));
        }

        wrapper.last("LIMIT " + (request.getTopK() != null ? request.getTopK() : 5));

        List<Knowledge> results = knowledgeMapper.selectList(wrapper);

        // 写入缓存（1年）
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(cacheKey, results, knowledgeCacheTtlHours, TimeUnit.HOURS);
            } catch (Exception e) {
                log.debug("写入知识库搜索缓存失败: {}", e.getMessage());
            }
        }

        return results;
    }

    @Override
    public String answerWithKnowledge(String question, Long scenicId) {
        KnowledgeRequest request = new KnowledgeRequest();
        request.setQuery(question);
        request.setScenicId(scenicId);
        request.setTopK(3);

        List<Knowledge> results = search(request);

        if (results.isEmpty()) {
            return null;
        }

        // 拼接知识库上下文
        return results.stream()
                .map(k -> String.format("【%s】%s", k.getTitle(), k.getContent()))
                .collect(Collectors.joining("\n\n"));
    }

    @Override
    public Knowledge addKnowledge(Knowledge knowledge) {
        knowledge.setEnabled(true);
        knowledge.setCreatedAt(LocalDateTime.now());
        knowledge.setUpdatedAt(LocalDateTime.now());
        knowledgeMapper.insert(knowledge);
        log.info("添加知识文档: id={}, title={}", knowledge.getId(), knowledge.getTitle());
        // 清除知识库缓存
        clearKnowledgeCache();
        return knowledge;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Knowledge> listKnowledge(String category, Long scenicId) {
        // 缓存
        String cacheKey = KNOWLEDGE_LIST_CACHE + category + ":" + scenicId;
        if (redisTemplate != null) {
            try {
                Object cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached != null) {
                    log.debug("知识库列表命中缓存");
                    return (List<Knowledge>) cached;
                }
            } catch (Exception e) {
                log.debug("读取知识库列表缓存失败: {}", e.getMessage());
            }
        }

        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Knowledge::getEnabled, true);
        if (category != null) wrapper.eq(Knowledge::getCategory, category);
        if (scenicId != null) wrapper.eq(Knowledge::getScenicId, scenicId);
        wrapper.orderByDesc(Knowledge::getUpdatedAt);
        List<Knowledge> results = knowledgeMapper.selectList(wrapper);

        // 写入缓存（1年）
        if (redisTemplate != null && !results.isEmpty()) {
            try {
                redisTemplate.opsForValue().set(cacheKey, results, knowledgeCacheTtlHours, TimeUnit.HOURS);
                log.info("知识库列表已缓存 [key={}]", cacheKey);
            } catch (Exception e) {
                log.debug("写入知识库列表缓存失败: {}", e.getMessage());
            }
        }
        return results;
    }

    @Override
    public void deleteKnowledge(Long id) {
        knowledgeMapper.deleteById(id);
        log.info("删除知识文档: id={}", id);
        // 清除知识库缓存
        clearKnowledgeCache();
    }

    /**
     * 清除所有知识库相关缓存
     */
    private void clearKnowledgeCache() {
        if (redisTemplate != null) {
            try {
                Set<String> keys = redisTemplate.keys(KNOWLEDGE_CACHE_PREFIX + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.info("清除知识库缓存: {} 个", keys.size());
                }
            } catch (Exception e) {
                log.debug("清除知识库缓存失败: {}", e.getMessage());
            }
        }
    }
}
