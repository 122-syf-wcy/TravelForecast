-- ============================================
-- 小程序轮播图独立表 + 确保商品表存在
-- ============================================

-- 1. 创建小程序专用轮播图表
CREATE TABLE IF NOT EXISTS mp_banners (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) COMMENT '标题',
    image VARCHAR(500) COMMENT '图片URL',
    link VARCHAR(500) COMMENT '跳转链接',
    sort INT DEFAULT 0 COMMENT '排序',
    enabled TINYINT(1) DEFAULT 1 COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '小程序轮播图表';

-- 2. 将 banners 表中的数据迁移到 mp_banners（如果 mp_banners 为空）
INSERT INTO mp_banners (id, title, image, link, sort, enabled)
SELECT id, title, image, link, sort, enabled
FROM banners
WHERE NOT EXISTS (SELECT 1 FROM mp_banners LIMIT 1);

-- 3. 确保商品表存在
CREATE TABLE IF NOT EXISTS mp_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    description VARCHAR(500) COMMENT '商品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    original_price DECIMAL(10,2) COMMENT '原价',
    image_url VARCHAR(500) COMMENT '商品图片',
    category VARCHAR(50) COMMENT '分类',
    tags VARCHAR(200) COMMENT '标签',
    stock INT DEFAULT 999 COMMENT '库存',
    sales INT DEFAULT 0 COMMENT '销量',
    qd_price INT COMMENT '黔豆兑换价格',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '小程序文创商品表';

-- 4. 插入初始商品数据（仅当表为空时）
INSERT INTO mp_products (name, description, price, category, tags, sales, qd_price, sort_order)
SELECT * FROM (
    SELECT '六盘水刺梨干 200g' as name, '维C之王·酸甜可口' as description, 29.90 as price, '地道美食' as category, '热销,特产' as tags, 2341 as sales, 50 as qd_price, 1 as sort_order
    UNION ALL SELECT '苗族蜡染围巾', '非遗手工·独一无二', 128.00, '非遗文创', '非遗,手工', 567, NULL, 2
    UNION ALL SELECT '水城羊肉粉速食包', '地道凉都味·5分钟搞定', 15.80, '地道美食', '爆款,美食', 5621, 30, 3
    UNION ALL SELECT '苦荞茶礼盒装', '高原好茶·送礼佳品', 68.00, '茶饮伴手', '茶叶,礼盒', 1205, 100, 4
    UNION ALL SELECT '苗族银饰耳环', '匠心手作·民族风情', 198.00, '非遗文创', '非遗,银饰', 312, NULL, 5
    UNION ALL SELECT '刺梨原浆果汁 6瓶', '天然VC·健康之选', 49.90, '地道美食', '热销,饮品', 3892, 80, 6
    UNION ALL SELECT '瑞昌剪纸明信片套装', '非遗艺术·精美收藏', 35.00, '非遗文创', '文创,明信片', 856, 60, 7
    UNION ALL SELECT '红心猕猴桃 2kg', '六盘水特产·新鲜直达', 59.90, '地道美食', '应季,水果', 4102, NULL, 8
) AS tmp
WHERE NOT EXISTS (SELECT 1 FROM mp_products LIMIT 1);
