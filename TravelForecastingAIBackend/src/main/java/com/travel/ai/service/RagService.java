package com.travel.ai.service;

import com.travel.ai.dto.KnowledgeRequest;
import com.travel.ai.entity.Knowledge;

import java.util.List;

/**
 * RAG知识库检索服务接口
 */
public interface RagService {

    /**
     * 知识库检索
     */
    List<Knowledge> search(KnowledgeRequest request);

    /**
     * 基于知识库的AI问答
     */
    String answerWithKnowledge(String question, Long scenicId);

    /**
     * 添加知识文档
     */
    Knowledge addKnowledge(Knowledge knowledge);

    /**
     * 获取知识文档列表
     */
    List<Knowledge> listKnowledge(String category, Long scenicId);

    /**
     * 删除知识文档
     */
    void deleteKnowledge(Long id);
}
