package com.travel.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时自动迁移数据：banners → mp_banners, 初始化 mp_products
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataMigration implements CommandLineRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        initSchema();
        migrateBanners();
        initProducts();
        initStudyContent();
    }

    private void initSchema() {
        try {
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS mp_cart (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    product_id BIGINT NOT NULL,
                    spec VARCHAR(100) DEFAULT NULL,
                    quantity INT NOT NULL DEFAULT 1,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_user_id (user_id),
                    INDEX idx_product_id (product_id)
                )
                """);
            jdbc.execute("ALTER TABLE mp_cart ADD COLUMN spec VARCHAR(100) DEFAULT NULL");
        } catch (Exception e) {
            log.debug("mp_cart spec 字段检查跳过: {}", e.getMessage());
        }

        try {
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS mp_orders (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    order_no VARCHAR(64) NOT NULL UNIQUE,
                    user_id BIGINT NOT NULL,
                    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
                    status VARCHAR(20) NOT NULL DEFAULT 'pending',
                    receiver_name VARCHAR(50) DEFAULT NULL,
                    receiver_phone VARCHAR(30) DEFAULT NULL,
                    receiver_address VARCHAR(255) DEFAULT NULL,
                    remark VARCHAR(255) DEFAULT NULL,
                    payment_time DATETIME DEFAULT NULL,
                    ship_time DATETIME DEFAULT NULL,
                    complete_time DATETIME DEFAULT NULL,
                    cancel_time DATETIME DEFAULT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_user_status (user_id, status),
                    INDEX idx_created_at (created_at)
                )
                """);
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS mp_order_items (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    order_id BIGINT NOT NULL,
                    cart_id BIGINT DEFAULT NULL,
                    product_id BIGINT DEFAULT NULL,
                    product_name VARCHAR(200) NOT NULL DEFAULT '',
                    product_image VARCHAR(500) DEFAULT NULL,
                    spec VARCHAR(100) DEFAULT NULL,
                    price DECIMAL(10,2) NOT NULL DEFAULT 0,
                    quantity INT NOT NULL DEFAULT 1,
                    INDEX idx_order_id (order_id)
                )
                """);
        } catch (Exception e) {
            log.debug("mp_order_items 建表检查跳过: {}", e.getMessage());
        }

        try {
            jdbc.execute("ALTER TABLE mp_order_items ADD COLUMN spec VARCHAR(100) DEFAULT NULL");
        } catch (Exception e) {
            log.debug("mp_order_items spec 字段检查跳过: {}", e.getMessage());
        }

        try {
            jdbc.execute("ALTER TABLE mp_order_items ADD COLUMN cart_id BIGINT DEFAULT NULL");
        } catch (Exception e) {
            log.debug("mp_order_items cart_id 字段检查跳过: {}", e.getMessage());
        }

        try {
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS mp_favorites (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    scenic_id BIGINT NOT NULL,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_user_scenic (user_id, scenic_id),
                    INDEX idx_user_id (user_id)
                )
                """);
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS mp_study_quiz (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    scenic_id BIGINT DEFAULT NULL,
                    scenic_name VARCHAR(100) DEFAULT NULL,
                    question VARCHAR(500) NOT NULL,
                    option_a VARCHAR(255) NOT NULL,
                    option_b VARCHAR(255) NOT NULL,
                    option_c VARCHAR(255) NOT NULL,
                    option_d VARCHAR(255) NOT NULL,
                    answer INT NOT NULL DEFAULT 0,
                    explanation VARCHAR(500) DEFAULT NULL,
                    difficulty VARCHAR(20) DEFAULT '基础',
                    category VARCHAR(50) DEFAULT '研学',
                    status VARCHAR(20) DEFAULT 'ACTIVE',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_scenic_name (scenic_name),
                    INDEX idx_status (status)
                )
                """);
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS mp_study_badge (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    icon_char VARCHAR(10) DEFAULT NULL,
                    color VARCHAR(50) DEFAULT NULL,
                    description VARCHAR(255) DEFAULT NULL,
                    condition_type VARCHAR(50) DEFAULT NULL,
                    condition_value INT DEFAULT 0,
                    sort_order INT DEFAULT 0
                )
                """);
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS mp_user_badge (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    badge_id BIGINT NOT NULL,
                    unlocked_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_user_badge (user_id, badge_id),
                    INDEX idx_user_id (user_id)
                )
                """);
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS mp_user_points (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL UNIQUE,
                    total_points INT NOT NULL DEFAULT 0,
                    used_points INT NOT NULL DEFAULT 0,
                    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                )
                """);
            jdbc.execute("""
                CREATE TABLE IF NOT EXISTS mp_study_answer_log (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    user_id BIGINT NOT NULL,
                    quiz_id BIGINT NOT NULL,
                    user_answer INT NOT NULL,
                    is_correct TINYINT(1) NOT NULL DEFAULT 0,
                    points INT NOT NULL DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_user_id (user_id),
                    INDEX idx_quiz_id (quiz_id)
                )
                """);
            log.info("小程序补充表结构检查完成");
        } catch (Exception e) {
            log.warn("小程序补充表结构初始化跳过: {}", e.getMessage());
        }
    }

    private void migrateBanners() {
        try {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM mp_banners", Integer.class);
            if (count != null && count > 0) {
                log.info("mp_banners 已有 {} 条数据，跳过迁移", count);
                return;
            }
            // 从旧 banners 表迁移
            int rows = jdbc.update(
                "INSERT INTO mp_banners (id, title, image, link, sort, enabled) " +
                "SELECT id, title, image, link, sort, enabled FROM banners"
            );
            log.info("从 banners 迁移 {} 条数据到 mp_banners", rows);
        } catch (Exception e) {
            log.warn("banners 数据迁移跳过: {}", e.getMessage());
        }
    }

    private void initProducts() {
        try {
            Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM mp_products", Integer.class);
            if (count != null && count > 0) {
                log.info("mp_products 已有 {} 条数据，跳过初始化", count);
                return;
            }
            String sql = "INSERT INTO mp_products (name, description, price, category, tags, sales, qd_price, sort_order) VALUES " +
                "('六盘水刺梨干 200g', '维C之王·酸甜可口', 29.90, '地道美食', '热销,特产', 2341, 50, 1)," +
                "('苗族蜡染围巾', '非遗手工·独一无二', 128.00, '非遗文创', '非遗,手工', 567, NULL, 2)," +
                "('水城羊肉粉速食包', '地道凉都味·5分钟搞定', 15.80, '地道美食', '爆款,美食', 5621, 30, 3)," +
                "('苦荞茶礼盒装', '高原好茶·送礼佳品', 68.00, '茶饮伴手', '茶叶,礼盒', 1205, 100, 4)," +
                "('苗族银饰耳环', '匠心手作·民族风情', 198.00, '非遗文创', '非遗,银饰', 312, NULL, 5)," +
                "('刺梨原浆果汁 6瓶', '天然VC·健康之选', 49.90, '地道美食', '热销,饮品', 3892, 80, 6)," +
                "('瑞昌剪纸明信片套装', '非遗艺术·精美收藏', 35.00, '非遗文创', '文创,明信片', 856, 60, 7)," +
                "('红心猕猴桃 2kg', '六盘水特产·新鲜直达', 59.90, '地道美食', '应季,水果', 4102, NULL, 8)";
            jdbc.update(sql);
            log.info("mp_products 初始数据插入完成");
        } catch (Exception e) {
            log.warn("mp_products 初始化跳过: {}", e.getMessage());
        }
    }

    private void initStudyContent() {
        try {
            Integer badgeCount = jdbc.queryForObject("SELECT COUNT(*) FROM mp_study_badge", Integer.class);
            if (badgeCount == null || badgeCount == 0) {
                jdbc.update("""
                    INSERT INTO mp_study_badge
                    (name, icon_char, color, description, condition_type, condition_value, sort_order)
                    VALUES
                    ('初识凉都', '初', '#2A9D8F', '完成一次研学答题', 'quiz_correct', 1, 1),
                    ('三线记忆', '线', '#E74C3C', '了解三线建设历史', 'quiz_correct', 3, 2),
                    ('研学达人', '研', '#FF9F43', '累计获得100黔豆', 'points', 100, 3),
                    ('凉都行者', '行', '#6C5CE7', '收藏或打卡多个景区', 'checkin', 3, 4)
                    """);
                log.info("mp_study_badge 初始数据插入完成");
            }

            Integer quizCount = jdbc.queryForObject("SELECT COUNT(*) FROM mp_study_quiz", Integer.class);
            if (quizCount != null && quizCount > 0) {
                log.info("mp_study_quiz 已有 {} 条数据，跳过初始化", quizCount);
                return;
            }
            jdbc.update("""
                INSERT INTO mp_study_quiz
                (scenic_id, scenic_name, question, option_a, option_b, option_c, option_d, answer, explanation, difficulty, category, status)
                VALUES
                (1, '梅花山风景区', '梅花山因什么旅游特色被称为凉都重要名片？', '高山滑雪与避暑', '海滨冲浪', '沙漠越野', '热带雨林探险', 0, '梅花山海拔较高，兼具避暑、赏梅和滑雪等山地旅游资源。', '基础', '景区文化', 'ACTIVE'),
                (2, '玉舍国家森林公园', '玉舍国家森林公园最突出的生态特征是？', '原始森林资源丰富', '大型海港码头', '沙漠绿洲景观', '火山地貌集中', 0, '玉舍以森林覆盖率高、森林氧吧和滑雪场等资源闻名。', '基础', '生态研学', 'ACTIVE'),
                (3, '乌蒙大草原', '乌蒙大草原被称为贵州屋脊，主要因为它具有哪种地理特征？', '海拔高、草原广阔', '临海潮汐明显', '地下溶洞密集', '热带雨林茂密', 0, '乌蒙大草原平均海拔较高，是贵州高山草原代表。', '基础', '地理研学', 'ACTIVE'),
                (4, '水城古镇', '水城古镇适合开展哪类研学主题？', '历史民俗与地方文化', '深海生物观察', '航天发射流程', '极地冰川科考', 0, '水城古镇保留历史街区和地方民俗，适合历史文化研学。', '基础', '历史研学', 'ACTIVE'),
                (5, '明湖国家湿地公园', '湿地公园最重要的生态价值之一是？', '维护生物多样性', '制造工业废水', '减少所有植物种类', '阻断城市绿地', 0, '湿地是多种动植物栖息地，对城市生态调节和生物多样性保护很重要。', '基础', '生态研学', 'ACTIVE')
                """);
            log.info("mp_study_quiz 初始数据插入完成");
        } catch (Exception e) {
            log.warn("研学内容初始化跳过: {}", e.getMessage());
        }
    }
}
