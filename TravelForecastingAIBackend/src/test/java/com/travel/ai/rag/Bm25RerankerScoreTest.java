package com.travel.ai.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Bm25RerankerScoreTest {

    private final Bm25Reranker reranker = new Bm25Reranker();

    @Test
    void rerankWithScore_returns_all_candidates_sorted() {
        record Doc(String id, String text) {}
        List<Doc> docs = List.of(
                new Doc("a", "三线建设博物馆 历史"),
                new Doc("b", "无关 内容"),
                new Doc("c", "三线建设 与红色研学")
        );

        List<Bm25Reranker.Scored<Doc>> scored = reranker.rerankWithScore(
                "三线建设",
                docs,
                Doc::text
        );

        assertEquals(3, scored.size(), "保留全部候选用于后续归一化");
        assertTrue(scored.get(0).score() >= scored.get(1).score());
        assertTrue(scored.get(1).score() >= scored.get(2).score());
        // 最相关的应该是 a 或 c，最末必然是 b
        assertEquals("b", scored.get(2).value().id());
    }

    @Test
    void rerankWithScore_returns_empty_when_query_blank() {
        record Doc(String id, String text) {}
        List<Doc> docs = List.of(new Doc("a", "abc"));
        assertTrue(reranker.rerankWithScore("", docs, Doc::text).isEmpty());
        assertTrue(reranker.rerankWithScore(null, docs, Doc::text).isEmpty());
    }
}
