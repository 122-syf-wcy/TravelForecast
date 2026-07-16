-- 优惠券表
CREATE TABLE IF NOT EXISTS `mp_coupons` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `name` VARCHAR(100) NOT NULL DEFAULT '',
  `type` VARCHAR(20) NOT NULL DEFAULT 'amount' COMMENT 'amount/percent',
  `discount` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `min_amount` DECIMAL(10,2) DEFAULT NULL,
  `condition` VARCHAR(100) DEFAULT '',
  `status` VARCHAR(20) NOT NULL DEFAULT 'valid' COMMENT 'valid/used/expired',
  `expire_time` DATETIME DEFAULT NULL,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user` (`user_id`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户优惠券';

-- 足迹表
CREATE TABLE IF NOT EXISTS `mp_footprints` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `user_id` BIGINT NOT NULL,
  `target_id` VARCHAR(50) NOT NULL,
  `target_type` VARCHAR(20) NOT NULL DEFAULT 'spot',
  `title` VARCHAR(200) DEFAULT '',
  `image_url` VARCHAR(500) DEFAULT '',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_user` (`user_id`),
  INDEX `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户足迹';
