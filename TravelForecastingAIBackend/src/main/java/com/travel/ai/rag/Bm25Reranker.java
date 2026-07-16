package com.travel.ai.rag;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * 轻量级 BM25 重排器。
 *
 * 在现有"SQL LIKE 召回"之后，对候选集做一次基于 BM25 的相关度排序，
 * 以提升 Top-K 命中率。针对中文场景采用 2-gram + 单字混合分词，
 * 既能保留实体（"梅花山"、"三线建设"）的切分又能容忍未登录词。
 *
 * 说明：
 * 1. 不引入外部向量库/embedding 依赖，可离线运行；
 * 2. 若后续引入 embedding 重排，可在本类之后再叠一层；
 * 3. 参数 k1=1.5, b=0.75 为通用默认值，可按需要暴露为配置。
 */
@Component
public class Bm25Reranker {

    private static final double K1 = 1.5;
    private static final double B = 0.75;

    /**
     * 对候选集按 query 相关度重排，返回 topK 命中。
     *
     * @param query         用户问题
     * @param candidates    召回候选集合
     * @param textExtractor 从候选对象抽取可检索文本（如 title+content+keywords）
     * @param topK          返回前 K 条
     * @param <T>           候选对象类型
     */
    public <T> List<T> rerank(String query, List<T> candidates, Function<T, String> textExtractor, int topK) {
        if (query == null || query.isEmpty() || candidates == null || candidates.isEmpty()) {
            return candidates == null ? List.of() : candidates;
        }

        List<Scored<T>> scored = rerankWithScore(query, candidates, textExtractor);

        int limit = Math.min(topK, scored.size());
        List<T> result = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++) {
            if (scored.get(i).score() <= 0) break;
            result.add(scored.get(i).value());
        }
        // 若所有 BM25 分数都是 0（通常是查询词在语料里全部未出现），
        // 回落到原始召回顺序，避免返回空列表。
        if (result.isEmpty()) {
            return candidates.subList(0, Math.min(topK, candidates.size()));
        }
        return result;
    }

    /**
     * 对候选集进行 BM25 打分并按分数倒序返回（不截断、不裁剪 score=0 项）。
     * 供 {@link HybridRetriever} 等上层模块进行归一化和加权融合。
     */
    public <T> List<Scored<T>> rerankWithScore(String query, List<T> candidates, Function<T, String> textExtractor) {
        if (query == null || query.isEmpty() || candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        List<List<String>> docs = new ArrayList<>(candidates.size());
        int totalTokens = 0;
        Map<String, Integer> df = new HashMap<>();

        for (T item : candidates) {
            String text = textExtractor.apply(item);
            List<String> tokens = tokenize(text);
            docs.add(tokens);
            totalTokens += tokens.size();

            for (String term : new java.util.HashSet<>(tokens)) {
                df.merge(term, 1, Integer::sum);
            }
        }

        double avgDl = docs.isEmpty() ? 0 : (double) totalTokens / docs.size();
        List<String> queryTokens = tokenize(query);
        int n = docs.size();

        List<Scored<T>> scored = new ArrayList<>(candidates.size());
        for (int i = 0; i < candidates.size(); i++) {
            double score = 0.0;
            List<String> doc = docs.get(i);
            int dl = doc.size();
            Map<String, Integer> tf = new HashMap<>();
            for (String term : doc) {
                tf.merge(term, 1, Integer::sum);
            }
            for (String qt : queryTokens) {
                int termDf = df.getOrDefault(qt, 0);
                if (termDf == 0) continue;
                double idf = Math.log(1.0 + (n - termDf + 0.5) / (termDf + 0.5));
                int f = tf.getOrDefault(qt, 0);
                if (f == 0) continue;
                double numerator = f * (K1 + 1);
                double denominator = f + K1 * (1 - B + B * (avgDl == 0 ? 1 : dl / avgDl));
                score += idf * numerator / denominator;
            }
            scored.add(new Scored<>(candidates.get(i), score));
        }

        scored.sort(Comparator.comparingDouble((Scored<T> s) -> s.score()).reversed());
        return scored;
    }

    /**
     * 简单的中文友好分词：
     * - 按非字母数字切分（保留英文/数字 whole word）
     * - 中文串按 2-gram + 单字打散
     * - 统一小写
     */
    public static List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) return List.of();
        List<String> tokens = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isCjk(c)) {
                if (buffer.length() > 0) {
                    tokens.add(buffer.toString().toLowerCase(Locale.ROOT));
                    buffer.setLength(0);
                }
                tokens.add(String.valueOf(c));
                if (i + 1 < text.length() && isCjk(text.charAt(i + 1))) {
                    tokens.add("" + c + text.charAt(i + 1));
                }
            } else if (Character.isLetterOrDigit(c)) {
                buffer.append(c);
            } else {
                if (buffer.length() > 0) {
                    tokens.add(buffer.toString().toLowerCase(Locale.ROOT));
                    buffer.setLength(0);
                }
            }
        }
        if (buffer.length() > 0) {
            tokens.add(buffer.toString().toLowerCase(Locale.ROOT));
        }
        return tokens;
    }

    private static boolean isCjk(char c) {
        return c >= 0x4E00 && c <= 0x9FFF;
    }

    /**
     * BM25 打分结果。提取为 public 是为了让 {@link HybridRetriever} 在
     * 包外消费分数；实际 record 定义不会被序列化或暴露给业务接口。
     */
    public record Scored<T>(T value, double score) {}
}
