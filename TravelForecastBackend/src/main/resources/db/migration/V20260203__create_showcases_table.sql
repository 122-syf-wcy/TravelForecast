-- 实景预览表（支持图片和视频）
CREATE TABLE IF NOT EXISTS showcases (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '标题',
    type VARCHAR(20) DEFAULT 'image' COMMENT '类型：image 或 video',
    url VARCHAR(500) NOT NULL COMMENT '媒体URL（图片或视频地址）',
    cover VARCHAR(500) COMMENT '封面图URL（视频类型时使用）',
    description VARCHAR(500) COMMENT '描述',
    sort INT DEFAULT 1 COMMENT '排序',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实景预览表';

-- 暂不插入默认数据，由管理员在后台上传
