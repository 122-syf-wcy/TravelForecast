package com.travel.ai.rag;

import com.alibaba.dashscope.embeddings.TextEmbedding;
import com.alibaba.dashscope.embeddings.TextEmbeddingOutput;
import com.alibaba.dashscope.embeddings.TextEmbeddingParam;
import com.alibaba.dashscope.embeddings.TextEmbeddingResult;
import com.alibaba.dashscope.embeddings.TextEmbeddingResultItem;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 文本向量化客户端。
 *
 * 优先调用 DashScope text-embedding-v2（1536 维，支持中文），
 * 避免引入额外的 Python 服务或大型本地模型，复用项目已有的 DashScope API Key。
 *
 * 行为约定：
 * 1. 若 `dashscope.api-key` 未配置，则 {@link #isEnabled()} 返回 false，
 *    上层 {@link RagServiceImpl} 自动降级为纯 BM25 检索；
 * 2. {@link #embedOne(String)} 对单条文本编码；{@link #embedMany(java.util.List)}
 *    批量编码，最多一次发送 25 条（DashScope 限制），超出自动拆批；
 * 3. 同一批中任意一条失败，全部返回空列表，调用方自行回退。
 */
@Component
public class EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingClient.class);

    /** DashScope 批量接口每次最多 25 条 */
    private static final int MAX_BATCH_SIZE = 25;

    @Value("${dashscope.api-key:}")
    private String apiKey;

    @Value("${dashscope.embedding-model:text-embedding-v2}")
    private String model;

    @Value("${ai.knowledge.embedding.enabled:true}")
    private boolean embeddingEnabled;

    private final TextEmbedding textEmbedding = new TextEmbedding();

    @PostConstruct
    public void init() {
        if (!isEnabled()) {
            log.warn("DashScope 未提供 api-key 或被显式关闭，向量化将禁用；RAG 将回退到纯 BM25 模式");
        } else {
            log.info("EmbeddingClient 已启用，model={}", model);
        }
    }

    public boolean isEnabled() {
        return embeddingEnabled && apiKey != null && !apiKey.isBlank();
    }

    public int getDimension() {
        return 1536;
    }

    /**
     * 单条文本向量化。失败或未启用时返回空 float[]。
     */
    public float[] embedOne(String text) {
        if (text == null || text.isEmpty() || !isEnabled()) {
            return new float[0];
        }
        List<float[]> list = embedMany(Collections.singletonList(text));
        return list.isEmpty() ? new float[0] : list.get(0);
    }

    /**
     * 批量向量化。顺序与入参对齐，失败时返回空列表。
     */
    public List<float[]> embedMany(List<String> texts) {
        if (texts == null || texts.isEmpty() || !isEnabled()) {
            return Collections.emptyList();
        }
        List<float[]> results = new ArrayList<>(texts.size());
        for (int start = 0; start < texts.size(); start += MAX_BATCH_SIZE) {
            int end = Math.min(start + MAX_BATCH_SIZE, texts.size());
            List<String> chunk = texts.subList(start, end);
            try {
                TextEmbeddingParam param = TextEmbeddingParam.builder()
                        .model(model)
                        .apiKey(apiKey)
                        .texts(chunk)
                        .build();
                TextEmbeddingResult result = textEmbedding.call(param);
                TextEmbeddingOutput output = result == null ? null : result.getOutput();
                if (output == null || output.getEmbeddings() == null) {
                    log.warn("DashScope 返回空 embedding，texts.size={}", chunk.size());
                    return Collections.emptyList();
                }
                List<TextEmbeddingResultItem> items = output.getEmbeddings();
                // 按 textIndex 排序，避免批内乱序
                items.sort((a, b) -> Integer.compare(
                        a.getTextIndex() == null ? 0 : a.getTextIndex(),
                        b.getTextIndex() == null ? 0 : b.getTextIndex()));
                for (TextEmbeddingResultItem item : items) {
                    List<Double> raw = item.getEmbedding();
                    if (raw == null) {
                        results.add(new float[0]);
                        continue;
                    }
                    float[] vec = new float[raw.size()];
                    for (int i = 0; i < raw.size(); i++) {
                        vec[i] = raw.get(i).floatValue();
                    }
                    results.add(vec);
                }
            } catch (ApiException | NoApiKeyException e) {
                log.error("调用 DashScope 向量化失败（将整体回退）: {}", e.getMessage());
                return Collections.emptyList();
            } catch (Exception e) {
                log.error("DashScope 向量化异常（将整体回退）: {}", e.getMessage());
                return Collections.emptyList();
            }
        }
        return results;
    }
}
