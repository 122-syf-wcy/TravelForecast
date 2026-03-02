-- ============================================
-- 小程序签到 & 收藏表 - 数据库迁移脚本
-- ============================================

-- 签到记录表
CREATE TABLE IF NOT EXISTS mp_checkin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    checkin_date DATE NOT NULL COMMENT '签到日期',
    points INT DEFAULT 5 COMMENT '获得积分(黔豆)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_date (user_id, checkin_date),
    INDEX idx_user_id (user_id)
) COMMENT '小程序签到记录表';

-- 收藏表
CREATE TABLE IF NOT EXISTS mp_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    scenic_id BIGINT NOT NULL COMMENT '景区ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_scenic (user_id, scenic_id),
    INDEX idx_user_id (user_id)
) COMMENT '小程序收藏表';
