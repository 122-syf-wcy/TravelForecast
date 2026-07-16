package com.travel.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.ai.dto.KnowledgeRequest;
import com.travel.ai.entity.Knowledge;
import com.travel.ai.mapper.KnowledgeMapper;
import com.travel.ai.rag.Bm25Reranker;
import com.travel.ai.rag.EmbeddingClient;
import com.travel.ai.rag.HybridRetriever;
import com.travel.ai.rag.VectorStore;
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
 * RAG知识库检索服务实现。
 *
 * 检索链路：
 * 1. 倒排召回：MyBatis-Plus LIKE 按 title / content / keywords 拿到最多
 *    `recall-size` 条候选（默认 20）；
 * 2. BM25 重排：{@link Bm25Reranker} 在 Java 内存中对候选集计算 BM25 分数，
 *    并按 topK 截断；比纯 ORDER BY updated_at 更贴近语义相关度；
 * 3. 结果缓存：查询/列表/答案都走 Redis 缓存，TTL 由 application.yml 控制。
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

    @Autowired
    private Bm25Reranker bm25Reranker;

    @Autowired(required = false)
    private HybridRetriever hybridRetriever;

    @Autowired(required = false)
    private VectorStore vectorStore;

    @Autowired(required = false)
    private EmbeddingClient embeddingClient;

    @Value("${ai.knowledge.cache-ttl-hours:8760}")
    private long knowledgeCacheTtlHours;

    @Value("${ai.knowledge.recall-size:20}")
    private int recallSize;

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

        int topK = request.getTopK() != null ? request.getTopK() : 5;
        int recallLimit = Math.max(recallSize, topK);

        // 基于关键词匹配：先召回较大的候选池（recallLimit），再由 BM25 重排
        if (request.getQuery() != null && !request.getQuery().isEmpty()) {
            wrapper.and(w -> w
                    .like(Knowledge::getTitle, request.getQuery())
                    .or()
                    .like(Knowledge::getContent, request.getQuery())
                    .or()
                    .like(Knowledge::getKeywords, request.getQuery()));
        }

        wrapper.last("LIMIT " + recallLimit);
        List<Knowledge> recall = knowledgeMapper.selectList(wrapper);

        List<Knowledge> results;
        if (request.getQuery() == null || request.getQuery().isEmpty()) {
            results = recall.stream().limit(topK).collect(Collectors.toList());
        } else if (hybridRetriever != null) {
            // 混合检索：BM25 + 向量重排（DashScope 可用时；否则等价于纯 BM25）
            List<HybridRetriever.Scored<Knowledge>> ranked = hybridRetriever.retrieve(
                    request.getQuery(),
                    recall,
                    k -> String.join(" \n ",
                            safe(k.getTitle()),
                            safe(k.getKeywords()),
                            safe(k.getContent())),
                    k -> k.getId() == null ? -1L : k.getId(),
                    topK);
            results = ranked.stream().map(HybridRetriever.Scored::value).collect(Collectors.toList());
            if (log.isDebugEnabled() && !ranked.isEmpty()) {
                HybridRetriever.Scored<Knowledge> top = ranked.get(0);
                log.debug("[RAG] hybrid top1 id={} bm25={} vec={} final={} useVec={}",
                        top.value().getId(), top.bm25Score(), top.vectorScore(), top.finalScore(), top.vectorUsed());
            }
        } else {
            results = bm25Reranker.rerank(
                    request.getQuery(),
                    recall,
                    k -> String.join(" \n ",
                            safe(k.getTitle()),
                            safe(k.getKeywords()),
                            safe(k.getContent())),
                    topK);
        }

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

    private static String safe(String s) {
        return s == null ? "" : s;
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
        // 清除知识库缓存 + 增量生成向量索引
        clearKnowledgeCache();
        upsertVector(knowledge);
        return knowledge;
    }

    /**
     * 同步把单条知识文档写入向量索引；EmbeddingClient 关闭时静默跳过。
     */
    private void upsertVector(Knowledge knowledge) {
        if (knowledge == null || knowledge.getId() == null) return;
        if (vectorStore == null || embeddingClient == null || !embeddingClient.isEnabled()) return;
        try {
            String text = String.join(" \n ",
                    safe(knowledge.getTitle()),
                    safe(knowledge.getKeywords()),
                    safe(knowledge.getContent()));
            float[] vec = embeddingClient.embedOne(text);
            if (vec.length > 0) {
                vectorStore.upsert(knowledge.getId(), vec);
            }
        } catch (Exception e) {
            log.warn("向量增量更新失败: id={}, error={}", knowledge.getId(), e.getMessage());
        }
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
        // 清除知识库缓存 + 同步删除向量索引
        clearKnowledgeCache();
        if (vectorStore != null && id != null) {
            try {
                vectorStore.delete(id);
            } catch (Exception e) {
                log.debug("向量索引删除失败: id={}, error={}", id, e.getMessage());
            }
        }
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
