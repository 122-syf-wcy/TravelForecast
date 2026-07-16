package com.travel.ai.rag;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class HybridRetrieverTest {

    record Doc(long id, String text) {}

    private Bm25Reranker bm25;
    private EmbeddingClient embeddingClient;
    private VectorStore vectorStore;
    private HybridRetriever retriever;

    @BeforeEach
    void setUp() {
        bm25 = new Bm25Reranker();
    }

    private HybridRetriever makeRetriever(EmbeddingClient ec, VectorStore vs) {
        HybridRetriever r = new HybridRetriever(bm25, ec, vs);
        ReflectionTestUtils.setField(r, "alphaVector", 0.6);
        return r;
    }

    @Test
    void should_fall_back_to_bm25_when_embedding_disabled() {
        embeddingClient = new EmbeddingClient() {
            @Override public boolean isEnabled() { return false; }
            @Override public float[] embedOne(String text) { return new float[0]; }
        };
        vectorStore = new VectorStore() {
            @Override public boolean isAvailable() { return false; }
        };
        retriever = makeRetriever(embeddingClient, vectorStore);

        List<Doc> candidates = List.of(
                new Doc(1, "三线建设博物馆位于六盘水"),
                new Doc(2, "乌蒙大草原 高山避暑")
        );
        List<HybridRetriever.Scored<Doc>> ranked = retriever.retrieve(
                "三线建设",
                candidates,
                Doc::text,
                Doc::id,
                3
        );

        assertFalse(ranked.isEmpty());
        assertEquals(1L, ranked.get(0).value().id());
        assertFalse(ranked.get(0).vectorUsed(), "应当显示未使用向量");
    }

    @Test
    void should_use_vector_when_embedding_available() {
        // 把 "三线建设" 的 query 向量与候选 1 的向量设为完全一致，候选 2 与 query 几乎正交
        AtomicReference<float[]> queryVec = new AtomicReference<>(new float[]{1f, 0f});

        embeddingClient = new EmbeddingClient() {
            @Override public boolean isEnabled() { return true; }
            @Override public float[] embedOne(String text) { return queryVec.get(); }
        };

        vectorStore = new VectorStore() {
            @Override public boolean isAvailable() { return true; }
            @Override public List<Match> search(float[] query, int topK, Set<Long> candidateIds) {
                // 模拟：id=1 的向量与 query 完全一致 → 1.0；id=2 与 query 正交 → 0.0
                return List.of(new Match(1L, 0.99f), new Match(2L, 0.01f));
            }
        };
        retriever = makeRetriever(embeddingClient, vectorStore);

        List<Doc> candidates = List.of(
                new Doc(1, "完全无关的字符串"),
                new Doc(2, "三线建设博物馆位于六盘水")
        );
        // BM25 会偏向 id=2（命中关键词）；向量重排会偏向 id=1。
        // 设置 alpha=0.6 时，向量权重压过 BM25，最终应是 id=1 居首。
        List<HybridRetriever.Scored<Doc>> ranked = retriever.retrieve(
                "三线建设",
                candidates,
                Doc::text,
                Doc::id,
                2
        );

        assertEquals(2, ranked.size());
        assertTrue(ranked.get(0).vectorUsed());
        assertEquals(1L, ranked.get(0).value().id());
    }

    @Test
    void should_dedupe_and_respect_topK() {
        embeddingClient = new EmbeddingClient() {
            @Override public boolean isEnabled() { return false; }
        };
        vectorStore = new VectorStore() {
            @Override public boolean isAvailable() { return false; }
        };
        retriever = makeRetriever(embeddingClient, vectorStore);

        List<Doc> candidates = List.of(
                new Doc(1, "三线建设"),
                new Doc(2, "三线建设博物馆"),
                new Doc(3, "梅花山")
        );
        List<HybridRetriever.Scored<Doc>> ranked = retriever.retrieve(
                "三线",
                candidates,
                Doc::text,
                Doc::id,
                2
        );
        assertEquals(2, ranked.size());
        // 至少前 2 项不应是梅花山
        assertNotEquals(3L, ranked.get(0).value().id());
    }
}
