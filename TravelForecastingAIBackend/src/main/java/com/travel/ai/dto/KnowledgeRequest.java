package com.travel.ai.dto;

import lombok.Data;

/**
 * 知识库查询请求DTO
 */
@Data
public class KnowledgeRequest {
    /** 查询内容 */
    private String query;
    /** 分类过滤 */
    private String category;
    /** 景区ID过滤 */
    private Long scenicId;
    /** 返回数量 */
    private Integer topK = 5;
}
