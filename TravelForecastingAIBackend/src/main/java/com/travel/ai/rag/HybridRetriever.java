package com.travel.ai.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToLongFunction;

/**
 * BM25 召回 × 向量语义重排的混合检索器。
 *
 * 调用流程：
 * 1. {@link Bm25Reranker} 基于中文 bigram 对 SQL LIKE 召回结果打分；
 * 2. 若 {@link EmbeddingClient#isEnabled()} 为真，则把 query 向量化，
 *    与 {@link VectorStore} 中候选文档的向量求余弦相似度；
 * 3. 按 {@code alphaVector} 线性加权 BM25 归一化分数与余弦相似度分数；
 * 4. 取 topK，保留调试用的 score/source 信息供日志或回答时溯源。
 *
 * 任一步骤失败都会优雅降级为纯 BM25，不会影响主流程。
 */
@Component
public class HybridRetriever {

    private static final Logger log = LoggerFactory.getLogger(HybridRetriever.class);

    private final Bm25Reranker bm25Reranker;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;

    @Value("${ai.knowledge.alpha-vector:0.55}")
    private double alphaVector;

    public HybridRetriever(Bm25Reranker bm25Reranker, EmbeddingClient embeddingClient, VectorStore vectorStore) {
        this.bm25Reranker = bm25Reranker;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
    }

    /**
     * 对候选集做混合重排。
     *
     * @param query        用户问题
     * @param candidates   BM25 召回的候选集
     * @param textExtractor 抽取可检索文本（title+content+keywords 等）
     * @param idExtractor  抽取主键，供向量 store 匹配
     * @param topK         返回 top K
     */
    public <T> List<Scored<T>> retrieve(
            String query,
            List<T> candidates,
            Function<T, String> textExtractor,
            ToLongFunction<T> idExtractor,
            int topK) {
        if (candidates == null || candidates.isEmpty()) return List.of();

        // 1) BM25 打分（保留全量以便后续归一化）
        List<Bm25Reranker.Scored<T>> bm25Scored = bm25Reranker.rerankWithScore(query, candidates, textExtractor);
        Map<T, Double> bm25Map = new HashMap<>();
        double bm25Max = 0;
        for (Bm25Reranker.Scored<T> s : bm25Scored) {
            bm25Map.put(s.value(), s.score());
            if (s.score() > bm25Max) bm25Max = s.score();
        }
        if (bm25Max <= 0) bm25Max = 1; // 避免除零

        // 2) 向量重排（可选）
        Map<T, Double> vectorMap = new HashMap<>();
        boolean vectorUsed = false;
        if (embeddingClient.isEnabled() && vectorStore.isAvailable()) {
            try {
                float[] queryVec = embeddingClient.embedOne(query);
                if (queryVec.length > 0) {
                    Set<Long> idSet = new HashSet<>();
                    Map<Long, T> idToCandidate = new HashMap<>();
                    for (T c : candidates) {
                        long id = idExtractor.applyAsLong(c);
                        idSet.add(id);
                        idToCandidate.put(id, c);
                    }
                    List<VectorStore.Match> matches = vectorStore.search(queryVec, candidates.size(), idSet);
                    for (VectorStore.Match m : matches) {
                        T cand = idToCandidate.get(m.id());
                        if (cand != null) {
                            vectorMap.put(cand, (double) m.score());
                        }
                    }
                    vectorUsed = !vectorMap.isEmpty();
                }
            } catch (Exception e) {
                log.warn("向量重排失败，降级为纯 BM25: {}", e.getMessage());
            }
        }

        // 3) 线性加权
        final double bm25Norm = bm25Max;
        final boolean useVec = vectorUsed;
        List<Scored<T>> merged = new ArrayList<>(candidates.size());
        for (T item : candidates) {
            double bm25 = bm25Map.getOrDefault(item, 0.0) / bm25Norm;
            double vec = vectorMap.getOrDefault(item, 0.0);
            double finalScore = useVec
                    ? alphaVector * vec + (1 - alphaVector) * bm25
                    : bm25;
            merged.add(new Scored<>(item, finalScore, bm25, vec, useVec));
        }
        merged.sort(Comparator.comparingDouble(Scored<T>::finalScore).reversed());

        // 4) 截取 topK，且过滤掉全 0 分项避免返回无关内容
        List<Scored<T>> top = new ArrayList<>();
        Set<T> seen = new LinkedHashSet<>();
        for (Scored<T> s : merged) {
            if (s.finalScore() <= 0) break;
            if (seen.add(s.value())) {
                top.add(s);
                if (top.size() >= topK) break;
            }
        }
        // 若所有分数为 0（极端情况），退化到 BM25 结果原序，保证至少返回候选
        if (top.isEmpty()) {
            for (int i = 0; i < Math.min(topK, candidates.size()); i++) {
                top.add(new Scored<>(candidates.get(i), 0, 0, 0, useVec));
            }
        }
        return top;
    }

    public record Scored<T>(T value, double finalScore, double bm25Score, double vectorScore, boolean vectorUsed) {}
}
