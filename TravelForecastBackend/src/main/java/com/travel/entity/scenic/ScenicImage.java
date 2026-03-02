package com.travel.entity.scenic;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 景区图片表
 */
@Data
@TableName("scenic_images")
public class ScenicImage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 图片ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 景区ID
     */
    private Long scenicId;

    /**
     * 图片URL
     */
    private String imageUrl;

    /**
     * 图片类型：COVER/GALLERY/MAP
     */
    private String imageType;

    /**
     * 排序
     */
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}

