-- ============================================
-- 子景点功能 - 数据库迁移脚本
-- 为 scenic_spots 表添加父子关系支持
-- ============================================

-- 1. 添加 parent_id 字段（NULL 表示主景区，非 NULL 表示子景点）
ALTER TABLE scenic_spots ADD COLUMN parent_id BIGINT NULL DEFAULT NULL COMMENT '父景区ID，NULL表示主景区' AFTER id;

-- 2. 添加 sort_order 字段（子景点排序）
ALTER TABLE scenic_spots ADD COLUMN sort_order INT NULL DEFAULT 0 COMMENT '排序序号' AFTER parent_id;

-- 3. 添加外键索引
ALTER TABLE scenic_spots ADD INDEX idx_parent_id (parent_id);

-- 4. 添加外键约束（可选，如果需要严格约束）
-- ALTER TABLE scenic_spots ADD CONSTRAINT fk_scenic_parent 
--   FOREIGN KEY (parent_id) REFERENCES scenic_spots(id) ON DELETE CASCADE;

-- ============================================
-- 示例数据（假设梅花山风景区 ID 为 1，请根据实际 ID 修改）
-- ============================================
-- INSERT INTO scenic_spots (parent_id, sort_order, name, description, image_url, status, city, category, created_at, updated_at)
-- VALUES
-- (1, 1, '梅花山滑雪场', '冬季滑雪胜地，拥有多条不同难度的雪道', NULL, 'ACTIVE', '六盘水', '休闲娱乐', NOW(), NOW()),
-- (1, 2, '梅花山观景台', '海拔2600米，可俯瞰整个梅花山全景', NULL, 'ACTIVE', '六盘水', '自然风光', NOW(), NOW()),
-- (1, 3, '梅花山休息区', '提供餐饮和休息服务的综合区域', NULL, 'ACTIVE', '六盘水', '休闲娱乐', NOW(), NOW());
