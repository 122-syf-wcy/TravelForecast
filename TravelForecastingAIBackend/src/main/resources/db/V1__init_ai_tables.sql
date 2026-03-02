-- ============================================================
-- 游韵华章 · AI智能服务 数据库初始化脚本
-- 在 travel_prediction 数据库中创建AI相关表
-- ============================================================

-- AI聊天会话表
CREATE TABLE IF NOT EXISTS `ai_conversations` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    `conversation_uuid` VARCHAR(64) NOT NULL COMMENT '会话UUID',
    `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
    `scenic_id` BIGINT DEFAULT NULL COMMENT '关联景区ID',
    `type` VARCHAR(20) DEFAULT 'chat' COMMENT '会话类型: chat/planning/education',
    `title` VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
    `last_message` TEXT DEFAULT NULL COMMENT '最后一条消息',
    `message_count` INT DEFAULT 0 COMMENT '消息数量',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 0=未删除, 1=已删除',
    UNIQUE KEY `uk_conversation_uuid` (`conversation_uuid`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_type` (`type`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天会话表';

-- AI聊天消息表
CREATE TABLE IF NOT EXISTS `ai_messages` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '消息ID',
    `conversation_id` BIGINT NOT NULL COMMENT '会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色: user/assistant/system',
    `content` LONGTEXT NOT NULL COMMENT '消息内容',
    `content_type` VARCHAR(20) DEFAULT 'text' COMMENT '消息类型: text/image/audio',
    `token_usage` INT DEFAULT NULL COMMENT 'Token消耗数',
    `model` VARCHAR(50) DEFAULT NULL COMMENT '使用的模型',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_conversation_id` (`conversation_id`),
    INDEX `idx_role` (`role`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天消息表';

-- AI知识库文档表
CREATE TABLE IF NOT EXISTS `ai_knowledge` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '文档ID',
    `title` VARCHAR(200) NOT NULL COMMENT '文档标题',
    `content` LONGTEXT NOT NULL COMMENT '文档内容',
    `category` VARCHAR(50) DEFAULT NULL COMMENT '分类: scenic/culture/food/transport/policy',
    `scenic_id` BIGINT DEFAULT NULL COMMENT '关联景区ID',
    `source` VARCHAR(200) DEFAULT NULL COMMENT '来源',
    `keywords` VARCHAR(500) DEFAULT NULL COMMENT '关键词(逗号分隔)',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用: 0=禁用, 1=启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX `idx_category` (`category`),
    INDEX `idx_scenic_id` (`scenic_id`),
    INDEX `idx_enabled` (`enabled`),
    FULLTEXT INDEX `ft_content` (`title`, `content`, `keywords`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI知识库文档表';

-- 研学教育路线表
CREATE TABLE IF NOT EXISTS `ai_study_routes` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '路线ID',
    `name` VARCHAR(200) NOT NULL COMMENT '路线名称',
    `description` TEXT DEFAULT NULL COMMENT '路线描述',
    `age_group` VARCHAR(20) DEFAULT 'all' COMMENT '适合年龄段: primary/middle/high/college/all',
    `theme` VARCHAR(50) DEFAULT NULL COMMENT '主题: geography/history/ecology/culture',
    `days` INT DEFAULT 1 COMMENT '路线天数',
    `route_detail` JSON DEFAULT NULL COMMENT '路线详情(JSON)',
    `learning_objectives` JSON DEFAULT NULL COMMENT '学习目标(JSON)',
    `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图片URL',
    `sort_order` INT DEFAULT 0 COMMENT '排序',
    `enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    INDEX `idx_age_group` (`age_group`),
    INDEX `idx_theme` (`theme`),
    INDEX `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='研学教育路线表';


-- ============================================================
-- 初始化知识库数据（六盘水旅游信息）
-- ============================================================

INSERT INTO `ai_knowledge` (`title`, `content`, `category`, `keywords`, `source`) VALUES
('梅花山风景区介绍', '梅花山风景区位于贵州省六盘水市钟山区，是六盘水的城市名片。景区以梅花观赏、生态休闲为特色，拥有万亩梅园、滑雪场、温泉等多种旅游资源。梅花山海拔2680米，是贵州海拔最高的景区之一。每年冬季的梅花盛开期间，吸引大量游客前来赏梅。门票价格：60元/人。', 'scenic', '梅花山,梅花,滑雪,温泉,钟山区', '六盘水市文旅局'),

('乌蒙大草原介绍', '乌蒙大草原位于贵州省六盘水市盘州市，被誉为"贵州屋脊"。草原平均海拔2857米，是贵州海拔最高的草原。这里有广袤的高山草原、壮观的风电场、独特的高原气候。适合户外徒步、骑马、露营等活动。门票价格：65元/人。', 'scenic', '乌蒙,草原,盘州,高原,风电', '六盘水市文旅局'),

('玉舍国家森林公园介绍', '玉舍国家森林公园位于六盘水市水城区，拥有原始森林、珍稀动植物等自然资源。公园内有滑雪场、森林步道、观景台等设施，四季皆宜旅游。冬季可滑雪，夏季可避暑，是六盘水重要的生态旅游景区。门票价格：40元/人。', 'scenic', '玉舍,森林,滑雪,水城区,生态', '六盘水市文旅局'),

('明湖国家湿地公园介绍', '明湖国家湿地公园位于六盘水市钟山区，是城市中的生态湿地。公园免费开放，适合市民休闲散步、观鸟、拍照。湿地生态系统完善，有丰富的水生植物和鸟类资源。', 'scenic', '明湖,湿地,免费,钟山区,生态', '六盘水市文旅局'),

('水城古镇介绍', '水城古镇位于六盘水市水城区，是一个具有悠久历史的古镇。古镇展现了当地彝族、苗族等少数民族的民俗风情，有特色建筑、传统手工艺、地方美食等。古镇免费开放。', 'scenic', '水城古镇,历史,彝族,苗族,民俗', '六盘水市文旅局'),

('六盘水气候特色', '六盘水素有"中国凉都"之称，夏季平均气温仅19.7℃，是中国避暑旅游的理想目的地。六盘水属亚热带季风湿润气候，四季分明，冬无严寒，夏无酷暑。最佳旅游季节为5-10月。', 'culture', '凉都,气候,避暑,温度,季节', '六盘水市政府'),

('六盘水特色美食', '六盘水美食以辣为主，代表性美食包括：水城烙锅（最具特色）、羊肉粉、烤豆腐、荞面、酸汤鱼等。水城烙锅是六盘水的名片美食，用特制的凹形砂锅烙制各种食材，味道鲜美独特。', 'food', '烙锅,羊肉粉,美食,特色小吃', '六盘水市文旅局'),

('六盘水交通指南', '六盘水交通便利：1. 航空：月照机场有直飞北京、上海、广州等城市航线；2. 铁路：六盘水站是西南地区重要铁路枢纽，有高铁和普速列车；3. 公路：沪昆高速、杭瑞高速经过六盘水；4. 市内交通：公交系统完善，出租车起步价5元。', 'transport', '交通,机场,高铁,公交,自驾', '六盘水市交通局');

-- 初始化研学路线数据
INSERT INTO `ai_study_routes` (`name`, `description`, `age_group`, `theme`, `days`, `sort_order`) VALUES
('乌蒙大草原地质探秘', '探索贵州屋脊的地质地貌，了解高原草原生态系统和风能发电原理', 'middle', 'geography', 2, 1),
('水城古镇民族文化之旅', '深入了解彝族苗族传统文化，体验非遗技艺和民族手工艺', 'primary', 'culture', 1, 2),
('玉舍森林生态研学', '走进原始森林，认识珍稀动植物，学习森林生态保护知识', 'high', 'ecology', 2, 3),
('六盘水凉都历史寻踪', '追溯六盘水的历史发展，了解三线建设的历史意义', 'college', 'history', 3, 4);
