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
        migrateBanners();
        initProducts();
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
}
