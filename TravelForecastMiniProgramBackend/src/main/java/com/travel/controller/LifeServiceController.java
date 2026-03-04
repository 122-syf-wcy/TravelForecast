package com.travel.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Result;
import com.travel.entity.News;
import com.travel.entity.Product;
import com.travel.entity.ScenicSpot;
import com.travel.mapper.NewsMapper;
import com.travel.mapper.ProductMapper;
import com.travel.mapper.ScenicSpotMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/life-services")
@RequiredArgsConstructor
public class LifeServiceController {

    private final ScenicSpotMapper scenicSpotMapper;
    private final ProductMapper productMapper;
    private final NewsMapper newsMapper;
    private final OssProxyController ossProxy;

    @GetMapping("/categories")
    public Result<List<Map<String, Object>>> categories() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(category("交通出行", "行", "linear-gradient(135deg, #dfe6e9, #b2bec3)"));
        list.add(category("酒店", "宿", "linear-gradient(135deg, #81ecec, #00cec9)"));
        list.add(category("美食", "食", "linear-gradient(135deg, #ffeaa7, #fdcb6e)"));
        list.add(category("非遗体验", "遗", "linear-gradient(135deg, #a29bfe, #6c5ce7)"));
        list.add(category("民宿", "居", "linear-gradient(135deg, #fd79a8, #e84393)"));
        list.add(category("门票", "门", "linear-gradient(135deg, #55efc4, #00b894)"));
        list.add(category("攻略", "略", "linear-gradient(135deg, #74b9ff, #0984e3)"));
        list.add(category("特色活动", "动", "linear-gradient(135deg, #fab1a0, #e17055)"));
        list.add(category("优惠福利", "惠", "linear-gradient(135deg, #ffeaa7, #f39c12)"));
        list.add(category("更多服务", "+", "linear-gradient(135deg, #dfe6e9, #b2bec3)"));
        return Result.success(list);
    }

    @GetMapping("/items")
    public Result<List<Map<String, Object>>> items(
            @RequestParam String category,
            @RequestParam(required = false) String keyword) {

        List<Map<String, Object>> list = switch (category) {
            case "交通出行" -> transportItems();
            case "酒店" -> hotelItems();
            case "美食" -> foodItems();
            case "非遗体验" -> heritageItems();
            case "民宿" -> homestayItems();
            case "门票" -> ticketItems();
            case "攻略" -> guideItems();
            case "特色活动" -> activityItems();
            case "优惠福利" -> benefitItems();
            case "更多服务" -> moreServiceItems();
            default -> new ArrayList<>();
        };

        return Result.success(filterByKeyword(list, keyword));
    }

    private List<Map<String, Object>> transportItems() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(staticItem(
                "交通出行", "transport-bus", "凉都旅游直通车", "景区-高铁站-市中心循环接驳，覆盖热门景点", "¥10起", "公交接驳",
                "navigate", null, "/pages/guide/index", "查看站点", null, "推荐"
        ));
        list.add(staticItem(
                "交通出行", "transport-rail", "高铁站接驳咨询", "六盘水站与重点景区接驳信息一键咨询", "按线路计费", "高铁联运",
                "phone", null, null, "电话咨询", "0858-1234567", null
        ));
        list.add(staticItem(
                "交通出行", "transport-drive", "自驾停车引导", "节假日停车位与分流线路实时指引", "免费", "自驾服务",
                "navigate", null, "/pages/guide/index", "打开地图", null, null
        ));
        return list;
    }

    private List<Map<String, Object>> hotelItems() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(staticItem(
                "酒店", "hotel-1", "凉都城景酒店", "距市中心商圈 5 分钟，支持景区接送与延迟退房", "¥268起", "舒适型",
                "phone", null, null, "预订咨询", "0858-3344556", "热门"
        ));
        list.add(staticItem(
                "酒店", "hotel-2", "梅花山度假酒店", "靠近梅花山景区，适合家庭出游与滑雪季入住", "¥398起", "度假型",
                "phone", null, null, "预订咨询", "0858-7688999", null
        ));
        list.add(staticItem(
                "酒店", "hotel-3", "乌蒙云上轻居", "交通便利，含早餐，支持开发票与行李寄存", "¥219起", "性价比",
                "phone", null, null, "预订咨询", "0858-5522331", null
        ));
        return list;
    }

    private List<Map<String, Object>> foodItems() {
        List<Product> products = filterProductsByKeyword(queryProducts(40), "美食", "饮", "粉", "特产", "茶");
        if (products.isEmpty()) {
            return fallbackFoodItems();
        }
        return products.stream()
                .limit(16)
                .map(p -> productItem("美食", p, p.getQdPrice() != null ? "去兑换" : "立即购买", "地道风味"))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> heritageItems() {
        List<Product> products = filterProductsByKeyword(queryProducts(40), "非遗", "手工", "蜡染", "银饰", "文创");
        if (products.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            list.add(staticItem(
                    "非遗体验", "heritage-1", "苗族蜡染手作体验", "2小时沉浸式手作课程，可带走个人作品", "¥98/人", "非遗手作",
                    "navigate", null, "/pages/red-study/index", "立即预约", null, "体验课"
            ));
            list.add(staticItem(
                    "非遗体验", "heritage-2", "银饰文化展示", "了解苗族银饰工艺与纹样寓意，支持现场定制", "免费参观", "民族工艺",
                    "navigate", null, "/pages/red-study/index", "查看详情", null, null
            ));
            return list;
        }
        return products.stream()
                .limit(16)
                .map(p -> productItem("非遗体验", p, "去看看", "非遗精选"))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> homestayItems() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(staticItem(
                "民宿", "homestay-1", "水城古镇里院民宿", "步行可达古镇夜景区，适合慢节奏深度游", "¥188起", "古镇风情",
                "phone", null, null, "预订咨询", "0858-6677881", "高评分"
        ));
        list.add(staticItem(
                "民宿", "homestay-2", "乌蒙云海木屋", "山景木屋，清晨观云海，支持围炉煮茶", "¥328起", "山景木屋",
                "phone", null, null, "预订咨询", "0858-6677882", null
        ));
        list.add(staticItem(
                "民宿", "homestay-3", "凉都花园民宿", "亲子友好，含接站服务与本地路线推荐", "¥238起", "亲子友好",
                "phone", null, null, "预订咨询", "0858-6677883", null
        ));
        return list;
    }

    private List<Map<String, Object>> ticketItems() {
        List<ScenicSpot> spots = queryMainSpots(30).stream()
                .filter(s -> s.getPrice() != null && !s.getPrice().isBlank())
                .collect(Collectors.toList());

        if (spots.isEmpty()) {
            spots = queryMainSpots(12);
        }

        return spots.stream()
                .limit(16)
                .map(s -> {
                    String priceText = firstNonBlank(s.getPrice(), "票价咨询");
                    String badge = firstNonBlank(s.getLevel(), "热门景区");
                    return spotItem("门票", s, priceText, "预约门票", badge);
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> guideItems() {
        List<News> allNews = queryPublishedNews(30);
        List<News> selected = filterNewsByKeyword(allNews, "攻略", "路线", "打卡", "游玩", "推荐");
        if (selected.isEmpty()) {
            selected = allNews;
        }

        if (selected.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            list.add(staticItem(
                    "攻略", "guide-1", "凉都一日游路线", "市区轻松路线：城市地标 + 特色美食 + 夜景", "建议时长1天", "轻松游",
                    "navigate", null, "/pages/digital-human/index", "AI定制", null, "智能推荐"
            ));
            return list;
        }

        return selected.stream()
                .limit(16)
                .map(n -> newsItem("攻略", n, "AI解读", "实用攻略"))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> activityItems() {
        List<News> allNews = queryPublishedNews(30);
        List<News> selected = filterNewsByKeyword(allNews, "活动", "节", "演出", "赛事", "展览");

        if (selected.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            list.add(staticItem(
                    "特色活动", "activity-1", "梅花山冬季滑雪季", "滑雪课程 + 亲子雪圈 + 夜场灯光秀", "按项目收费", "季节活动",
                    "navigate", null, "/pages/guide/index", "查看详情", null, "当季推荐"
            ));
            list.add(staticItem(
                    "特色活动", "activity-2", "凉都非遗文化周", "蜡染、银饰、彝族歌舞沉浸式体验", "免费参与", "文化活动",
                    "navigate", null, "/pages/red-study/index", "查看详情", null, null
            ));
            return list;
        }

        return selected.stream()
                .limit(16)
                .map(n -> newsItem("特色活动", n, "查看活动", "活动速递"))
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> benefitItems() {
        List<Product> allProducts = queryProducts(40);
        List<Product> selected = allProducts.stream()
                .filter(p -> p.getQdPrice() != null || hasDiscountPrice(p.getOriginalPrice(), p.getPrice()))
                .collect(Collectors.toList());

        if (selected.isEmpty()) {
            selected = allProducts;
        }

        if (selected.isEmpty()) {
            List<Map<String, Object>> list = new ArrayList<>();
            list.add(staticItem(
                    "优惠福利", "benefit-1", "新人礼包", "新用户可领景区周边满减券与文创优惠券", "限时领取", "新客福利",
                    "navigate", null, "/pages/profile/index", "立即领取", null, "限时"
            ));
            return list;
        }

        return selected.stream()
                .limit(16)
                .map(p -> {
                    String badge = p.getQdPrice() != null ? "黔豆可兑" : "限时优惠";
                    return productItem("优惠福利", p, p.getQdPrice() != null ? "去兑换" : "去购买", badge);
                })
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> moreServiceItems() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(staticItem(
                "更多服务", "more-1", "我的订单", "查看待付款、待使用、已完成订单", "实时同步", "订单中心",
                "navigate", null, "/pages/order/list", "立即查看", null, null
        ));
        list.add(staticItem(
                "更多服务", "more-2", "我的行程", "管理 AI 生成行程与手动规划行程", "行程管家", "行程服务",
                "navigate", null, "/pages/itinerary/list", "立即查看", null, null
        ));
        list.add(staticItem(
                "更多服务", "more-3", "智游地图", "景点导航、周边推荐、路线指引", "地图服务", "出行",
                "switchTab", null, "/pages/guide/index", "打开地图", null, "常用"
        ));
        list.add(staticItem(
                "更多服务", "more-4", "AI 数字伴游", "语音问答、行程建议、文化讲解", "7x24在线", "AI服务",
                "navigate", null, "/pages/digital-human/index", "立即咨询", null, "智能"
        ));
        return list;
    }

    private List<Map<String, Object>> fallbackFoodItems() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(staticItem(
                "美食", "food-1", "水城羊肉粉", "凉都地道风味，推荐搭配刺梨饮品", "¥18起", "本地必吃",
                "switchTab", null, "/pages/shop/index", "去购买", null, "热销"
        ));
        list.add(staticItem(
                "美食", "food-2", "刺梨原浆", "高维C饮品，景区伴手礼热门选择", "¥49.9", "伴手礼",
                "switchTab", null, "/pages/shop/index", "去购买", null, null
        ));
        return list;
    }

    private List<Map<String, Object>> filterByKeyword(List<Map<String, Object>> source, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return source;
        }
        String kw = keyword.trim().toLowerCase(Locale.ROOT);
        return source.stream()
                .filter(item -> containsKeyword(item.get("title"), kw)
                        || containsKeyword(item.get("summary"), kw)
                        || containsKeyword(item.get("tag"), kw)
                        || containsKeyword(item.get("badge"), kw))
                .collect(Collectors.toList());
    }

    private boolean containsKeyword(Object value, String keyword) {
        return value != null && value.toString().toLowerCase(Locale.ROOT).contains(keyword);
    }

    private List<Product> queryProducts(int limit) {
        LambdaQueryWrapper<Product> qw = new LambdaQueryWrapper<>();
        qw.eq(Product::getStatus, "ACTIVE")
                .orderByDesc(Product::getSales)
                .orderByAsc(Product::getSortOrder)
                .last("LIMIT " + limit);
        return productMapper.selectList(qw);
    }

    private List<ScenicSpot> queryMainSpots(int limit) {
        LambdaQueryWrapper<ScenicSpot> qw = new LambdaQueryWrapper<>();
        qw.isNull(ScenicSpot::getParentId)
                .eq(ScenicSpot::getStatus, "ACTIVE")
                .orderByDesc(ScenicSpot::getRating)
                .last("LIMIT " + limit);
        return scenicSpotMapper.selectList(qw);
    }

    private List<News> queryPublishedNews(int limit) {
        LambdaQueryWrapper<News> qw = new LambdaQueryWrapper<>();
        qw.eq(News::getStatus, "published")
                .orderByDesc(News::getPublishTime)
                .last("LIMIT " + limit);
        return newsMapper.selectList(qw);
    }

    private List<Product> filterProductsByKeyword(List<Product> source, String... keywords) {
        if (keywords == null || keywords.length == 0) {
            return source;
        }
        List<String> keys = Arrays.stream(keywords)
                .filter(k -> k != null && !k.isBlank())
                .map(k -> k.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());

        return source.stream()
                .filter(p -> matchesAny(p.getName(), keys)
                        || matchesAny(p.getCategory(), keys)
                        || matchesAny(p.getTags(), keys))
                .collect(Collectors.toList());
    }

    private List<News> filterNewsByKeyword(List<News> source, String... keywords) {
        if (keywords == null || keywords.length == 0) {
            return source;
        }
        List<String> keys = Arrays.stream(keywords)
                .filter(k -> k != null && !k.isBlank())
                .map(k -> k.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());

        return source.stream()
                .filter(n -> matchesAny(n.getTitle(), keys)
                        || matchesAny(n.getSummary(), keys)
                        || matchesAny(n.getCategory(), keys))
                .collect(Collectors.toList());
    }

    private boolean matchesAny(String content, List<String> keys) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String val = content.toLowerCase(Locale.ROOT);
        for (String key : keys) {
            if (val.contains(key)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> productItem(String category, Product p, String actionText, String badge) {
        String summary = firstNonBlank(p.getDescription(), "文旅好物精选");
        String priceText = formatPrice(p.getPrice());
        if (p.getQdPrice() != null) {
            priceText = priceText + " / " + p.getQdPrice() + "豆";
        }

        if (hasDiscountPrice(p.getOriginalPrice(), p.getPrice())) {
            priceText = priceText + "（原价" + formatPrice(p.getOriginalPrice()) + "）";
        }

        return item(
                p.getId(), category, p.getName(), summary,
                priceText,
                firstTag(p.getTags()),
                proxyImage(p.getImageUrl()),
                "shop", p.getId(), null, actionText, null, badge
        );
    }

    private Map<String, Object> spotItem(String category, ScenicSpot s, String priceText, String actionText, String badge) {
        return item(
                s.getId(), category, s.getName(),
                firstNonBlank(s.getDescription(), s.getAddress(), "热门景区推荐"),
                priceText,
                firstNonBlank(s.getCategory(), "景区"),
                proxyImage(s.getImageUrl()),
                "spot", s.getId(), null, actionText, null, badge
        );
    }

    private Map<String, Object> newsItem(String category, News n, String actionText, String badge) {
        return item(
                n.getId(), category, n.getTitle(),
                firstNonBlank(n.getSummary(), "最新文旅资讯"),
                "",
                firstNonBlank(n.getCategory(), category),
                "",
                "navigate", null, "/pages/digital-human/index", actionText, null, badge
        );
    }

    private Map<String, Object> staticItem(
            String category,
            String id,
            String title,
            String summary,
            String priceText,
            String tag,
            String targetType,
            Object targetId,
            String targetUrl,
            String actionText,
            String phone,
            String badge) {

        return item(id, category, title, summary, priceText, tag, "", targetType, targetId, targetUrl, actionText, phone, badge);
    }

    private Map<String, Object> item(
            Object id,
            String category,
            String title,
            String summary,
            String priceText,
            String tag,
            String imageUrl,
            String targetType,
            Object targetId,
            String targetUrl,
            String actionText,
            String phone,
            String badge) {

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("category", category);
        map.put("title", title);
        map.put("summary", summary);
        map.put("priceText", priceText);
        map.put("tag", tag);
        map.put("imageUrl", imageUrl);
        map.put("targetType", targetType);
        map.put("targetId", targetId);
        map.put("targetUrl", targetUrl);
        map.put("actionText", actionText);
        map.put("phone", phone);
        map.put("badge", badge);
        return map;
    }

    private Map<String, Object> category(String name, String charText, String bg) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", name);
        map.put("char", charText);
        map.put("bg", bg);
        return map;
    }

    private String proxyImage(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "";
        }
        return ossProxy.toProxyUrl(imageUrl);
    }

    private String firstTag(String tags) {
        if (tags == null || tags.isBlank()) {
            return "";
        }
        String[] arr = tags.split(",");
        return arr.length > 0 ? arr[0].trim() : tags;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String val : values) {
            if (val != null && !val.isBlank()) {
                return val;
            }
        }
        return "";
    }

    private String formatPrice(BigDecimal price) {
        if (price == null) {
            return "";
        }
        return "¥" + price.stripTrailingZeros().toPlainString();
    }

    private boolean hasDiscountPrice(BigDecimal originalPrice, BigDecimal currentPrice) {
        return originalPrice != null && currentPrice != null && originalPrice.compareTo(currentPrice) > 0;
    }
}
