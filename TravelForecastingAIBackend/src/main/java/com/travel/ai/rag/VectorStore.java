package com.travel.ai.rag;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 轻量级向量存储。
 *
 * 目标：提供"可用、可答辩、不引入新中间件"的向量检索能力。
 * 实现要点：
 * 1. 不依赖 Chroma / Milvus：利用项目已有的 Redis（数据库 1）存储，
 *    key 形如 `ai:vec:knowledge:{id}`，value 为向量 JSON；
 * 2. 通过 SET `ai:vec:knowledge:ids` 维护全量 id 列表，便于召回时遍历；
 * 3. 余弦相似度在应用内存计算，1k~5k 规模下足够（知识库实际更小）。
 *
 * 若要升级为真向量索引（IVF/HNSW），可将本类替换为 Chroma/Milvus 客户端，
 * 对外 API（{@link #upsert}, {@link #search}）保持不变即可无感替换。
 */
@Component
public class VectorStore {

    private static final Logger log = LoggerFactory.getLogger(VectorStore.class);

    private static final String VECTOR_KEY_PREFIX = "ai:vec:knowledge:";
    private static final String IDS_KEY = "ai:vec:knowledge:ids";

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${ai.knowledge.vector-ttl-hours:168}")
    private long vectorTtlHours;

    public boolean isAvailable() {
        return redisTemplate != null;
    }

    /**
     * 写入单条向量。同时刷新 IDS_KEY 的 TTL，保证 set 与向量过期时间一致，
     * 避免向量过期后仍被 search 扫描到 null。
     */
    public void upsert(long id, float[] vector) {
        if (redisTemplate == null || vector == null || vector.length == 0) return;
        try {
            List<Float> payload = new ArrayList<>(vector.length);
            for (float v : vector) {
                payload.add(v);
            }
            redisTemplate.opsForValue().set(
                    VECTOR_KEY_PREFIX + id,
                    JSON.toJSONString(payload),
                    vectorTtlHours,
                    TimeUnit.HOURS
            );
            redisTemplate.opsForSet().add(IDS_KEY, String.valueOf(id));
            // 取较长的 TTL（向量 TTL + 1 天），防止与单条向量同时过期导致空窗
            redisTemplate.expire(IDS_KEY, vectorTtlHours + 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("写入向量失败: id={}, error={}", id, e.getMessage());
        }
    }

    /**
     * 批量写入。
     */
    public void upsertAll(Map<Long, float[]> vectors) {
        if (vectors == null || vectors.isEmpty()) return;
        vectors.forEach(this::upsert);
    }

    /**
     * 删除（主要用于知识库 deleteById 时同步清理）。
     */
    public void delete(long id) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(VECTOR_KEY_PREFIX + id);
            redisTemplate.opsForSet().remove(IDS_KEY, String.valueOf(id));
        } catch (Exception e) {
            log.warn("删除向量失败: id={}, error={}", id, e.getMessage());
        }
    }

    /**
     * 按向量相似度检索，返回 topK 命中项与分数。
     * 对 {@code candidateIds} 过滤，只在给定集合内匹配；传 null 表示全库扫描。
     */
    public List<Match> search(float[] query, int topK, Set<Long> candidateIds) {
        if (redisTemplate == null || query == null || query.length == 0 || topK <= 0) {
            return Collections.emptyList();
        }
        try {
            Set<String> ids = redisTemplate.opsForSet().members(IDS_KEY);
            if (ids == null || ids.isEmpty()) return Collections.emptyList();

            List<Match> matches = new ArrayList<>();
            // 收集"过期 / 解析失败"的脏 id，统一从 set 里清掉，避免下次扫描浪费 IO
            List<String> staleIds = new ArrayList<>();
            for (String raw : ids) {
                long id;
                try {
                    id = Long.parseLong(raw);
                } catch (NumberFormatException e) {
                    staleIds.add(raw);
                    continue;
                }
                if (candidateIds != null && !candidateIds.contains(id)) {
                    continue;
                }
                String val = redisTemplate.opsForValue().get(VECTOR_KEY_PREFIX + id);
                if (val == null) {
                    staleIds.add(raw);
                    continue;
                }
                List<Float> payload = JSON.parseObject(val, new TypeReference<List<Float>>() {});
                if (payload == null || payload.size() != query.length) {
                    staleIds.add(raw);
                    continue;
                }
                float score = cosine(payload, query);
                matches.add(new Match(id, score));
            }
            if (!staleIds.isEmpty()) {
                try {
                    redisTemplate.opsForSet().remove(IDS_KEY, (Object[]) staleIds.toArray(new String[0]));
                } catch (Exception ignore) {
                    // 清理失败不影响主流程
                }
            }
            matches.sort(Comparator.comparingDouble(Match::score).reversed());
            return matches.stream().limit(topK).collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("向量检索异常: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    public Map<Long, float[]> loadAll() {
        if (redisTemplate == null) return new HashMap<>();
        Set<String> ids = redisTemplate.opsForSet().members(IDS_KEY);
        if (ids == null || ids.isEmpty()) return new HashMap<>();
        Map<Long, float[]> res = new HashMap<>(ids.size());
        for (String raw : ids) {
            try {
                long id = Long.parseLong(raw);
                String val = redisTemplate.opsForValue().get(VECTOR_KEY_PREFIX + id);
                if (val == null) continue;
                List<Float> payload = JSON.parseObject(val, new TypeReference<List<Float>>() {});
                if (payload == null) continue;
                float[] vec = new float[payload.size()];
                for (int i = 0; i < payload.size(); i++) vec[i] = payload.get(i);
                res.put(id, vec);
            } catch (Exception ignore) {
                // 单条失败不影响整体
            }
        }
        return res;
    }

    /**
     * 向量库是否已有内容（用于决定是否需要触发初始化任务）。
     */
    public long size() {
        if (redisTemplate == null) return 0L;
        Long c = redisTemplate.opsForSet().size(IDS_KEY);
        return c == null ? 0L : c;
    }

    private float cosine(List<Float> a, float[] b) {
        float dot = 0f, na = 0f, nb = 0f;
        for (int i = 0; i < b.length; i++) {
            float x = a.get(i);
            float y = b[i];
            dot += x * y;
            na += x * x;
            nb += y * y;
        }
        if (na == 0 || nb == 0) return 0f;
        return (float) (dot / (Math.sqrt(na) * Math.sqrt(nb)));
    }

    public record Match(long id, float score) {}
}
