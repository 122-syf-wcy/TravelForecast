-- 用户反馈表
CREATE TABLE IF NOT EXISTS `mp_feedback` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `type` VARCHAR(50) NOT NULL DEFAULT '其他' COMMENT '反馈类型：功能异常/体验问题/内容纠错/功能建议',
  `content` TEXT NOT NULL COMMENT '反馈内容',
  `contact` VARCHAR(100) DEFAULT '' COMMENT '联系方式',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '处理状态：pending/resolved/rejected',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user_id` (`user_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='小程序用户反馈';
