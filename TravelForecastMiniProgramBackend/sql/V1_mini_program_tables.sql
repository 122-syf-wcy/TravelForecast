-- ============================================
-- 小程序专用表 - 数据库迁移脚本
-- 数据库: travel_prediction (与主后端共用)
-- ============================================

-- 1. 文创商品表
CREATE TABLE IF NOT EXISTS mp_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    description VARCHAR(500) COMMENT '商品描述',
    price DECIMAL(10,2) NOT NULL COMMENT '价格',
    original_price DECIMAL(10,2) COMMENT '原价',
    image_url VARCHAR(500) COMMENT '商品图片',
    category VARCHAR(50) COMMENT '分类: 非遗文创/地道美食/茶饮伴手/手工艺品',
    tags VARCHAR(200) COMMENT '标签，逗号分隔',
    stock INT DEFAULT 999 COMMENT '库存',
    sales INT DEFAULT 0 COMMENT '销量',
    qd_price INT COMMENT '黔豆兑换价格(可选)',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '小程序文创商品表';

-- 2. 购物车表
CREATE TABLE IF NOT EXISTS mp_cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID(wechat_users.user_id)',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    quantity INT DEFAULT 1 COMMENT '数量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) COMMENT '小程序购物车表';

-- 3. 商城订单表
CREATE TABLE IF NOT EXISTS mp_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单编号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '订单总金额',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending/paid/shipped/completed/cancelled',
    receiver_name VARCHAR(50) COMMENT '收货人',
    receiver_phone VARCHAR(20) COMMENT '收货电话',
    receiver_address VARCHAR(300) COMMENT '收货地址',
    remark VARCHAR(200) COMMENT '备注',
    payment_time DATETIME COMMENT '支付时间',
    ship_time DATETIME COMMENT '发货时间',
    complete_time DATETIME COMMENT '完成时间',
    cancel_time DATETIME COMMENT '取消时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) COMMENT '小程序商城订单表';

-- 4. 订单明细表
CREATE TABLE IF NOT EXISTS mp_order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    product_id BIGINT COMMENT '商品ID',
    product_name VARCHAR(200) NOT NULL COMMENT '商品名称',
    product_image VARCHAR(500) COMMENT '商品图片',
    price DECIMAL(10,2) NOT NULL COMMENT '单价',
    quantity INT DEFAULT 1 COMMENT '数量',
    INDEX idx_order_id (order_id)
) COMMENT '小程序订单明细表';

-- 5. 研学题库表
CREATE TABLE IF NOT EXISTS mp_study_quiz (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scenic_id BIGINT COMMENT '关联景区ID(可选)',
    scenic_name VARCHAR(100) COMMENT '景区名称',
    question VARCHAR(500) NOT NULL COMMENT '题目',
    option_a VARCHAR(200) NOT NULL,
    option_b VARCHAR(200) NOT NULL,
    option_c VARCHAR(200) NOT NULL,
    option_d VARCHAR(200) NOT NULL,
    answer INT NOT NULL COMMENT '正确答案索引(0-3)',
    explanation VARCHAR(500) COMMENT '答案解析',
    difficulty VARCHAR(20) DEFAULT '简单' COMMENT '难度: 简单/中等/困难',
    category VARCHAR(50) COMMENT '分类: 历史/地理/文化/自然',
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '小程序研学题库表';

-- 6. 用户答题记录表
CREATE TABLE IF NOT EXISTS mp_study_answer_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    quiz_id BIGINT NOT NULL,
    user_answer INT NOT NULL COMMENT '用户选择的答案索引',
    is_correct TINYINT(1) NOT NULL COMMENT '是否正确',
    points INT DEFAULT 0 COMMENT '获得积分',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) COMMENT '小程序答题记录表';

-- 7. 研学护照(徽章)表
CREATE TABLE IF NOT EXISTS mp_study_badge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '徽章名称',
    icon_char VARCHAR(10) COMMENT '图标文字',
    color VARCHAR(20) COMMENT '颜色',
    description VARCHAR(200) COMMENT '解锁条件描述',
    condition_type VARCHAR(50) COMMENT '条件类型: login/checkin/quiz/purchase/visit',
    condition_value INT DEFAULT 1 COMMENT '条件值',
    sort_order INT DEFAULT 0
) COMMENT '小程序研学徽章定义表';

-- 8. 用户徽章解锁记录
CREATE TABLE IF NOT EXISTS mp_user_badge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    badge_id BIGINT NOT NULL,
    unlocked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_badge (user_id, badge_id),
    INDEX idx_user_id (user_id)
) COMMENT '小程序用户徽章表';

-- 9. 用户积分表
CREATE TABLE IF NOT EXISTS mp_user_points (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    total_points INT DEFAULT 0 COMMENT '总积分(黔豆)',
    used_points INT DEFAULT 0 COMMENT '已使用积分',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) COMMENT '小程序用户积分表';

-- 10. 行程表
CREATE TABLE IF NOT EXISTS mp_itineraries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(100) COMMENT '行程标题',
    days INT DEFAULT 1 COMMENT '天数',
    status VARCHAR(20) DEFAULT 'planning' COMMENT '状态: planning/ongoing/completed',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) COMMENT '小程序行程表';

-- 11. 行程节点表
CREATE TABLE IF NOT EXISTS mp_itinerary_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    itinerary_id BIGINT NOT NULL,
    day_num INT DEFAULT 1 COMMENT '第几天',
    time_slot VARCHAR(20) COMMENT '时间段如10:00',
    title VARCHAR(100) NOT NULL COMMENT '节点标题',
    description VARCHAR(300) COMMENT '描述',
    scenic_id BIGINT COMMENT '关联景区ID',
    image_url VARCHAR(500) COMMENT '图片',
    sort_order INT DEFAULT 0,
    INDEX idx_itinerary_id (itinerary_id)
) COMMENT '小程序行程节点表';

-- ============================================
-- 初始数据
-- ============================================

-- 研学题库初始数据
INSERT INTO mp_study_quiz (scenic_name, question, option_a, option_b, option_c, option_d, answer, explanation, difficulty, category) VALUES
('三线建设博物馆', '"三线建设"是从哪一年开始的？', '1958年', '1964年', '1970年', '1978年', 1, '1964年，中央决定进行大规模的三线建设，将沿海地区的工业向内地转移。', '简单', '历史'),
('三线建设博物馆', '六盘水因三线建设而兴起的主要工业是什么？', '纺织工业', '煤炭和钢铁工业', '电子工业', '汽车工业', 1, '六盘水因煤炭资源丰富，三线建设时期大力发展煤炭和钢铁工业，被称为"江南煤都"。', '简单', '历史'),
('三线建设博物馆', '"三线"指的是什么地区？', '东北三省', '中国内陆腹地', '长三角地区', '珠三角地区', 1, '三线地区指中国内陆腹地，包括四川、贵州、云南、陕西等省份。', '中等', '历史'),
('梅花山景区', '梅花山滑雪场的海拔约为多少米？', '1800米', '2200米', '2680米', '3100米', 2, '梅花山滑雪场海拔约2680米，是世界上纬度最低的天然滑雪场之一。', '简单', '地理'),
('六盘水', '六盘水被称为什么？', '春城', '中国凉都', '雾都', '冰城', 1, '六盘水夏季平均气温19.7℃，2005年被中国气象学会授予"中国凉都"称号。', '简单', '地理'),
('乌蒙大草原', '乌蒙大草原位于六盘水的哪个区县？', '钟山区', '盘州市', '水城区', '六枝特区', 1, '乌蒙大草原位于盘州市，是西南地区海拔最高、面积最大的高原草场之一。', '中等', '地理'),
('水城古镇', '水城古镇有多少年的历史？', '200多年', '400多年', '600多年', '800多年', 2, '水城古镇始建于明朝洪武年间，距今已有600多年历史。', '中等', '历史'),
('六盘水', '"六盘水"这个名字的由来是什么？', '一座山的名字', '六枝、盘县、水城三地合称', '一条河的名字', '一位历史人物的名字', 1, '六盘水由六枝特区、盘县（今盘州市）、水城县（今水城区）三地名各取一字组成。', '简单', '文化'),
('六盘水', '六盘水的市花是什么？', '杜鹃花', '玉兰花', '桂花', '梅花', 0, '六盘水的市花是杜鹃花，每年春天百里杜鹃竞相开放，蔚为壮观。', '中等', '自然'),
('六盘水', '六盘水属于哪个民族自治州？', '不属于自治州，是地级市', '黔南州', '黔西南州', '黔东南州', 0, '六盘水是贵州省辖地级市，不属于任何自治州。', '中等', '文化');

-- 研学徽章初始数据
INSERT INTO mp_study_badge (name, icon_char, color, description, condition_type, condition_value, sort_order) VALUES
('初来乍到', '初', '#0984e3', '首次登录小程序', 'login', 1, 1),
('三线学者', '线', '#ee5a24', '完成三线建设博物馆研学答题', 'quiz', 3, 2),
('美食猎人', '味', '#f6b93b', '购买美食类商品', 'purchase', 1, 3),
('凉都达人', '凉', '#00b894', '打卡3个景区', 'visit', 3, 4),
('文创大师', '创', '#6c5ce7', '购买文创商品', 'purchase', 1, 5),
('全景解锁', '全', '#F39C12', '打卡全部景区', 'visit', 10, 6);

-- 文创商品初始数据
INSERT INTO mp_products (name, description, price, category, tags, sales, qd_price, sort_order) VALUES
('六盘水刺梨干 200g', '维C之王·酸甜可口', 29.90, '地道美食', '热销,特产', 2341, 50, 1),
('苗族蜡染围巾', '非遗手工·独一无二', 128.00, '非遗文创', '非遗,手工', 567, NULL, 2),
('水城羊肉粉速食包', '地道凉都味·5分钟搞定', 15.80, '地道美食', '爆款,美食', 5621, 30, 3),
('苦荞茶礼盒装', '高原好茶·送礼佳品', 68.00, '茶饮伴手', '茶叶,礼盒', 1205, 100, 4),
('苗族银饰耳环', '匠心手作·民族风情', 198.00, '非遗文创', '非遗,银饰', 312, NULL, 5),
('刺梨原浆果汁 6瓶', '天然VC·健康之选', 49.90, '地道美食', '热销,饮品', 3892, 80, 6),
('瑞昌剪纸明信片套装', '非遗艺术·精美收藏', 35.00, '非遗文创', '文创,明信片', 856, 60, 7),
('红心猕猴桃 2kg', '六盘水特产·新鲜直达', 59.90, '地道美食', '应季,水果', 4102, NULL, 8);
