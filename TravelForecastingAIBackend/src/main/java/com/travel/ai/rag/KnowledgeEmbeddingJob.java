package com.travel.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.ai.entity.Knowledge;
import com.travel.ai.mapper.KnowledgeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 知识库 → 向量索引 一次性同步任务。
 *
 * 触发时机：Spring Boot 启动完成（{@link ApplicationReadyEvent}）后异步执行；
 * 不阻塞 HTTP 端口启动，确保 health 探活不被向量构建拖慢。
 *
 * 行为：
 * 1. EmbeddingClient 未启用 / VectorStore 不可用时 → 直接 return
 * 2. 已经存在向量（基于 ids set size 判定）→ 跳过初始化，避免重复消耗 token
 * 3. 否则按 batch=20 调 DashScope 批量 embedding，写入 Redis
 *
 * 想强制重建：清空 Redis 中 `ai:vec:knowledge:*`，然后重启服务即可。
 */
@Component
public class KnowledgeEmbeddingJob {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeEmbeddingJob.class);

    private final KnowledgeMapper knowledgeMapper;
    private final EmbeddingClient embeddingClient;
    private final VectorStore vectorStore;
    private final Executor taskExecutor;

    @Value("${ai.knowledge.embedding.batch-size:20}")
    private int batchSize;

    @Value("${ai.knowledge.embedding.bootstrap:true}")
    private boolean bootstrapEnabled;

    public KnowledgeEmbeddingJob(KnowledgeMapper knowledgeMapper,
                                  EmbeddingClient embeddingClient,
                                  VectorStore vectorStore,
                                  @Qualifier("aiTaskExecutor") Executor taskExecutor) {
        this.knowledgeMapper = knowledgeMapper;
        this.embeddingClient = embeddingClient;
        this.vectorStore = vectorStore;
        this.taskExecutor = taskExecutor;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        if (!bootstrapEnabled) {
            log.info("知识库向量化引导已被禁用 (ai.knowledge.embedding.bootstrap=false)");
            return;
        }
        if (!embeddingClient.isEnabled() || !vectorStore.isAvailable()) {
            log.info("EmbeddingClient/VectorStore 未就绪，跳过向量化引导");
            return;
        }
        taskExecutor.execute(this::buildIfMissing);
    }

    /** 真正的同步逻辑，外部测试可以直接调用 */
    public int buildIfMissing() {
        long existing = vectorStore.size();
        if (existing > 0) {
            log.info("向量库已存在 {} 条索引，跳过 bootstrap", existing);
            return 0;
        }
        return rebuildAll();
    }

    public int rebuildAll() {
        if (!embeddingClient.isEnabled() || !vectorStore.isAvailable()) return 0;

        LambdaQueryWrapper<Knowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Knowledge::getEnabled, true)
                .orderByAsc(Knowledge::getId);
        List<Knowledge> all = knowledgeMapper.selectList(wrapper);
        if (all.isEmpty()) {
            log.info("知识库为空，无需向量化");
            return 0;
        }

        log.info("开始知识库向量化: total={}", all.size());
        int processed = 0;
        int failedBatches = 0;
        List<Long> failedIds = new ArrayList<>();
        for (int start = 0; start < all.size(); start += batchSize) {
            int end = Math.min(start + batchSize, all.size());
            List<Knowledge> batch = all.subList(start, end);
            List<String> texts = new ArrayList<>(batch.size());
            for (Knowledge k : batch) {
                texts.add(toEmbeddingText(k));
            }
            List<float[]> vecs = embeddingClient.embedMany(texts);
            if (vecs.isEmpty() || vecs.size() != batch.size()) {
                // 单批失败：记录失败 id，跳过当前批继续，避免整体放弃
                failedBatches++;
                for (Knowledge k : batch) {
                    if (k.getId() != null) failedIds.add(k.getId());
                }
                log.warn("批次向量化失败 batch[{}~{})，已跳过本批继续后续", start, end);
                continue;
            }
            Map<Long, float[]> toUpsert = new HashMap<>(batch.size());
            for (int i = 0; i < batch.size(); i++) {
                Knowledge k = batch.get(i);
                if (k.getId() != null && vecs.get(i).length > 0) {
                    toUpsert.put(k.getId(), vecs.get(i));
                }
            }
            vectorStore.upsertAll(toUpsert);
            processed += toUpsert.size();
            log.info("向量化进度: {}/{}", processed, all.size());
        }
        if (failedBatches > 0) {
            log.warn("知识库向量化部分失败: 失败批次={} 失败id={}", failedBatches, failedIds);
        }
        log.info("知识库向量化完成: 写入 {}/{} 条", processed, all.size());
        return processed;
    }

    private String toEmbeddingText(Knowledge k) {
        return String.join(" \n ",
                safe(k.getTitle()),
                safe(k.getKeywords()),
                safe(k.getContent()));
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
