package com.travel.entity.scenic;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评价回复表（商家回复）
 */
@Data
@TableName("review_replies")
public class ReviewReply implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 评价ID
     */
    private Long reviewId;

    /**
     * 回复用户ID（通常是商家）
     */
    private Long userId;

    /**
     * 回复内容
     */
    private String content;

    /**
     * 状态: published/hidden/deleted
     */
    private String status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 软删除
     */
    @TableLogic
    private LocalDateTime deletedAt;
}

