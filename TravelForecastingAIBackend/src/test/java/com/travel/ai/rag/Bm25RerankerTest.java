package com.travel.ai.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class Bm25RerankerTest {

    private final Bm25Reranker reranker = new Bm25Reranker();

    @Test
    void tokenize_should_split_chinese_bigram_and_english() {
        List<String> tokens = Bm25Reranker.tokenize("三线建设 AI 讲解");
        assertTrue(tokens.contains("三"));
        assertTrue(tokens.contains("线"));
        assertTrue(tokens.contains("三线"));
        assertTrue(tokens.contains("建设"));
        assertTrue(tokens.contains("ai"));
        assertTrue(tokens.contains("讲解"));
    }

    @Test
    void rerank_should_prefer_documents_matching_query_semantics() {
        record Doc(String title, String content) {}
        List<Doc> candidates = List.of(
                new Doc("水城古镇", "水城古镇位于贵州六盘水，以三线建设历史闻名，三线建设博物馆在此驻馆。"),
                new Doc("乌蒙大草原", "海拔 2800 米的高山草甸，夏季避暑胜地。"),
                new Doc("梅花山风景区", "规划春季花海、缆车与观景台，夏季避暑胜地。")
        );

        List<Doc> ranked = reranker.rerank(
                "三线建设历史",
                candidates,
                d -> d.title() + " " + d.content(),
                3
        );

        assertFalse(ranked.isEmpty());
        // "三线建设" 语料里只有水城古镇一篇命中，BM25 应把它排到第一
        assertEquals("水城古镇", ranked.get(0).title());
    }

    @Test
    void rerank_should_fallback_to_original_order_when_no_match() {
        record Doc(String id, String content) {}
        List<Doc> candidates = List.of(
                new Doc("a", "无关内容 alpha"),
                new Doc("b", "无关内容 bravo")
        );

        List<Doc> ranked = reranker.rerank(
                "完全不相关的查询",
                candidates,
                Doc::content,
                5
        );

        assertEquals(2, ranked.size());
        assertEquals("a", ranked.get(0).id());
    }
}
