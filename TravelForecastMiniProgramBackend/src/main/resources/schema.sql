-- 小程序专用轮播图表
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

-- 小程序文创商品表
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

-- 用户反馈表
CREATE TABLE IF NOT EXISTS mp_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT DEFAULT NULL COMMENT '用户ID',
    type VARCHAR(50) NOT NULL DEFAULT '其他' COMMENT '反馈类型',
    content TEXT NOT NULL COMMENT '反馈内容',
    contact VARCHAR(100) DEFAULT '' COMMENT '联系方式',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '处理状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) COMMENT '小程序用户反馈';

-- 用户收货地址表
CREATE TABLE IF NOT EXISTS mp_user_address (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(50) NOT NULL COMMENT '收货人姓名',
    phone VARCHAR(20) NOT NULL COMMENT '手机号',
    province VARCHAR(50) NOT NULL DEFAULT '' COMMENT '省',
    city VARCHAR(50) NOT NULL DEFAULT '' COMMENT '市',
    district VARCHAR(50) NOT NULL DEFAULT '' COMMENT '区',
    detail VARCHAR(255) NOT NULL DEFAULT '' COMMENT '详细地址',
    is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认地址',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) COMMENT '小程序用户收货地址';

-- 小程序购物车表
CREATE TABLE IF NOT EXISTS mp_cart (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    spec VARCHAR(100) DEFAULT NULL COMMENT '商品规格',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_product_id (product_id)
) COMMENT '小程序购物车表';

-- 小程序订单表
CREATE TABLE IF NOT EXISTS mp_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL UNIQUE COMMENT '订单号',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '订单金额',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '订单状态',
    receiver_name VARCHAR(50) DEFAULT NULL COMMENT '收货人',
    receiver_phone VARCHAR(30) DEFAULT NULL COMMENT '收货电话',
    receiver_address VARCHAR(255) DEFAULT NULL COMMENT '收货地址',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    payment_time DATETIME DEFAULT NULL,
    ship_time DATETIME DEFAULT NULL,
    complete_time DATETIME DEFAULT NULL,
    cancel_time DATETIME DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_status (user_id, status),
    INDEX idx_created_at (created_at)
) COMMENT '小程序订单表';

-- 小程序订单明细表
CREATE TABLE IF NOT EXISTS mp_order_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL COMMENT '订单ID',
    cart_id BIGINT DEFAULT NULL COMMENT '来源购物车ID',
    product_id BIGINT DEFAULT NULL COMMENT '商品ID',
    product_name VARCHAR(200) NOT NULL DEFAULT '' COMMENT '商品名称',
    product_image VARCHAR(500) DEFAULT NULL COMMENT '商品图片',
    spec VARCHAR(100) DEFAULT NULL COMMENT '商品规格',
    price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '单价',
    quantity INT NOT NULL DEFAULT 1 COMMENT '数量',
    INDEX idx_order_id (order_id)
) COMMENT '小程序订单明细表';

-- 小程序收藏表
CREATE TABLE IF NOT EXISTS mp_favorites (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    scenic_id BIGINT NOT NULL COMMENT '景区ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_scenic (user_id, scenic_id),
    INDEX idx_user_id (user_id)
) COMMENT '小程序收藏表';

-- 小程序研学题库
CREATE TABLE IF NOT EXISTS mp_study_quiz (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scenic_id BIGINT DEFAULT NULL COMMENT '景区ID',
    scenic_name VARCHAR(100) DEFAULT NULL COMMENT '景区名称',
    question VARCHAR(500) NOT NULL COMMENT '题目',
    option_a VARCHAR(255) NOT NULL COMMENT '选项A',
    option_b VARCHAR(255) NOT NULL COMMENT '选项B',
    option_c VARCHAR(255) NOT NULL COMMENT '选项C',
    option_d VARCHAR(255) NOT NULL COMMENT '选项D',
    answer INT NOT NULL DEFAULT 0 COMMENT '正确答案下标，0=A',
    explanation VARCHAR(500) DEFAULT NULL COMMENT '解析',
    difficulty VARCHAR(20) DEFAULT '基础' COMMENT '难度',
    category VARCHAR(50) DEFAULT '研学' COMMENT '分类',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_scenic_name (scenic_name),
    INDEX idx_status (status)
) COMMENT '小程序研学题库';

-- 小程序研学徽章
CREATE TABLE IF NOT EXISTS mp_study_badge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '徽章名称',
    icon_char VARCHAR(10) DEFAULT NULL COMMENT '徽章字',
    color VARCHAR(50) DEFAULT NULL COMMENT '颜色',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    condition_type VARCHAR(50) DEFAULT NULL COMMENT '解锁条件类型',
    condition_value INT DEFAULT 0 COMMENT '解锁条件值',
    sort_order INT DEFAULT 0 COMMENT '排序'
) COMMENT '小程序研学徽章';

-- 小程序用户徽章
CREATE TABLE IF NOT EXISTS mp_user_badge (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    badge_id BIGINT NOT NULL COMMENT '徽章ID',
    unlocked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_badge (user_id, badge_id),
    INDEX idx_user_id (user_id)
) COMMENT '小程序用户徽章';

-- 小程序用户积分
CREATE TABLE IF NOT EXISTS mp_user_points (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    total_points INT NOT NULL DEFAULT 0 COMMENT '累计积分',
    used_points INT NOT NULL DEFAULT 0 COMMENT '已使用积分',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '小程序用户积分';

-- 小程序研学答题记录
CREATE TABLE IF NOT EXISTS mp_study_answer_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    quiz_id BIGINT NOT NULL COMMENT '题目ID',
    user_answer INT NOT NULL COMMENT '用户答案',
    is_correct TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否正确',
    points INT NOT NULL DEFAULT 0 COMMENT '获得积分',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_quiz_id (quiz_id)
) COMMENT '小程序研学答题记录';
