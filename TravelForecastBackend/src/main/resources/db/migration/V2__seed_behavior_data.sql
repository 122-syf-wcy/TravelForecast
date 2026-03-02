-- 修复用户行为日志表缺失并在插入数据前建表

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 1. 创建表结构 (如果不存在)
-- ----------------------------
DROP TABLE IF EXISTS `user_behavior_logs`;
CREATE TABLE `user_behavior_logs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint(20) DEFAULT NULL COMMENT '用户ID',
  `session_id` varchar(64) DEFAULT NULL COMMENT '会话ID',
  `behavior_type` varchar(32) DEFAULT NULL COMMENT '行为类型: page_view, click, search等',
  `module` varchar(64) DEFAULT NULL COMMENT '模块/页面名称',
  `target_id` bigint(20) DEFAULT NULL COMMENT '目标ID',
  `duration` int(11) DEFAULT NULL COMMENT '停留时长(秒)',
  `device_type` varchar(32) DEFAULT NULL COMMENT '设备类型',
  `ip_address` varchar(64) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` varchar(255) DEFAULT NULL COMMENT 'User Agent',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户行为日志表';

-- ----------------------------
-- 2. 插入模拟数据
-- ----------------------------
INSERT INTO `user_behavior_logs` (`user_id`, `behavior_type`, `module`, `duration`, `created_at`) VALUES 
(1, 'page_view', 'dashboard', 120, DATE_SUB(NOW(), INTERVAL 1 HOUR)),
(2, 'page_view', 'scenic', 300, DATE_SUB(NOW(), INTERVAL 2 HOUR)),
(3, 'page_view', 'prediction', 180, DATE_SUB(NOW(), INTERVAL 3 HOUR)),
(4, 'page_view', 'planning', 600, DATE_SUB(NOW(), INTERVAL 5 HOUR)),
(5, 'page_view', 'dashboard', 60, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(6, 'page_view', 'scenic', 450, DATE_SUB(NOW(), INTERVAL 1 DAY)),
(1, 'page_view', 'ai', 120, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(2, 'page_view', 'profile', 30, DATE_SUB(NOW(), INTERVAL 2 DAY)),
(3, 'click', 'landing', 15, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(4, 'click', 'login', 45, DATE_SUB(NOW(), INTERVAL 3 DAY)),
(5, 'page_view', 'dashboard', 90, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(6, 'page_view', 'scenic', 240, DATE_SUB(NOW(), INTERVAL 4 DAY)),
(1, 'page_view', 'prediction', 200, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(2, 'page_view', 'planning', 500, DATE_SUB(NOW(), INTERVAL 5 DAY)),
(3, 'page_view', 'service', 150, DATE_SUB(NOW(), INTERVAL 6 DAY)),
(4, 'page_view', 'dashboard', 100, DATE_SUB(NOW(), INTERVAL 7 DAY)),
(5, 'page_view', 'scenic', 350, DATE_SUB(NOW(), INTERVAL 8 DAY)),
(6, 'page_view', 'prediction', 160, DATE_SUB(NOW(), INTERVAL 9 DAY)),
(1, 'page_view', 'planning', 480, DATE_SUB(NOW(), INTERVAL 10 DAY)),
(2, 'page_view', 'dashboard', 70, DATE_SUB(NOW(), INTERVAL 11 DAY)),
(3, 'page_view', 'scenic', 400, DATE_SUB(NOW(), INTERVAL 12 DAY)),
(4, 'page_view', 'ai', 110, DATE_SUB(NOW(), INTERVAL 13 DAY)),
(5, 'page_view', 'profile', 40, DATE_SUB(NOW(), INTERVAL 14 DAY)),
(6, 'click', 'landing', 20, DATE_SUB(NOW(), INTERVAL 15 DAY));

-- 批量生成更多数据
INSERT INTO `user_behavior_logs` (`user_id`, `behavior_type`, `module`, `duration`, `created_at`)
SELECT 
    FLOOR(1 + RAND() * 10),
    'page_view',
    ELT(FLOOR(1 + RAND() * 6), 'dashboard', 'scenic', 'prediction', 'planning', 'ai', 'profile'),
    FLOOR(10 + RAND() * 600),
    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
FROM information_schema.tables LIMIT 100;

SET FOREIGN_KEY_CHECKS = 1;
