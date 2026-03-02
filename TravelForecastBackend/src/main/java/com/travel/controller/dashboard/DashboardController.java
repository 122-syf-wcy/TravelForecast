package com.travel.controller.dashboard;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.common.Result;
import com.travel.dto.dashboard.PopularSpot;
import com.travel.dto.dashboard.ScenicFlow;
import com.travel.dto.dashboard.TourismNews;
import com.travel.dto.admin.UserStats;
import com.travel.dto.dashboard.WeatherInfo;
import com.travel.entity.content.News;
import com.travel.entity.scenic.ScenicImage;
import com.travel.entity.scenic.ScenicReview;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.entity.scenic.ScenicStatistics;
import com.travel.entity.user.UserFavorite;
import com.travel.mapper.content.NewsMapper;
import com.travel.mapper.scenic.ScenicReviewMapper;
import com.travel.mapper.scenic.ScenicStatisticsMapper;
import com.travel.service.prediction.PredictionService;
import com.travel.service.scenic.ScenicImageService;
import com.travel.service.scenic.ScenicSpotService;
import com.travel.service.user.UserFavoriteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired(required = false)
    private ScenicSpotService scenicSpotService;

    @Autowired(required = false)
    private NewsMapper newsMapper;

    @Autowired(required = false)
    private ScenicReviewMapper scenicReviewMapper;

    @Autowired(required = false)
    private UserFavoriteService userFavoriteService;

    @Autowired(required = false)
    private ScenicImageService scenicImageService;

    @Autowired(required = false)
    private ScenicStatisticsMapper scenicStatisticsMapper;

    @Autowired(required = false)
    private PredictionService predictionService;

    @Value("${app.oss.use-direct-url:true}")
    private boolean useDirectOssUrl;

    @Value("${app.weather.amap.key:339146bdb9038c3caf85a7aca9c9bb7f}")
    private String amapWeatherKey;

    @Value("${app.weather.amap.city-code:520200}")
    private String amapCityCode;

    /**
     * 健康检查接口
     * GET /dashboard/ping
     */
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("pong");
    }

    /**
     * 获取景区实时流量数据（优先使用预测模型）
     * GET /dashboard/scenic-flow
     */
    @GetMapping("/scenic-flow")
    public Result<List<ScenicFlow>> getScenicFlow() {
        log.info("获取景区实时流量数据");

        try {
            if (scenicSpotService == null) {
                log.warn("ScenicSpotService未注入，返回空列表");
                return Result.success(new ArrayList<>());
            }

            List<ScenicSpot> spots = scenicSpotService.getAllActive();

            LocalDate today = LocalDate.now();
            int currentHour = LocalTime.now().getHour();

            List<ScenicFlow> flowData = spots.parallelStream()
                    .map(spot -> {
                        ScenicFlow flow = new ScenicFlow();
                        flow.setId(spot.getId());
                        flow.setName(spot.getName());

                        int maxCapacity = spot.getMaxCapacity() != null ? spot.getMaxCapacity() : 2000;
                        flow.setMaxCapacity(maxCapacity);

                        int currentFlow = 0;
                        boolean usedPrediction = false;

                        // 1. 尝试调用预测服务获取当前小时的预测流量
                        if (predictionService != null) {
                            try {
                                Map<String, Object> prediction = predictionService
                                        .predictHourlyDistribution(today, spot.getId(), "dual_stream");
                                if (prediction != null && prediction.containsKey("hours")
                                        && prediction.containsKey("visitors")) {
                                    @SuppressWarnings("unchecked")
                                    List<String> hours = (List<String>) prediction.get("hours");
                                    @SuppressWarnings("unchecked")
                                    List<Integer> visitors = (List<Integer>) prediction.get("visitors");

                                    int index = -1;
                                    for (int i = 0; i < hours.size(); i++) {
                                        if (hours.get(i).startsWith(String.format("%02d", currentHour))) {
                                            index = i;
                                            break;
                                        }
                                    }

                                    if (index != -1 && index < visitors.size()) {
                                        currentFlow = visitors.get(index);
                                        usedPrediction = true;
                                        log.debug("景区 {} 使用预测模型数据: {} ({}点)", spot.getName(), currentFlow, currentHour);
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("调用预测服务失败，将使用降级方案: {}", e.getMessage());
                            }
                        }

                        // 2. 如果预测服务不可用或失败，使用数据库统计数据或估算
                        if (!usedPrediction) {
                            currentFlow = estimateCurrentFlow(spot, today, maxCapacity);
                        }

                        flow.setCurrentFlow(currentFlow);
                        flow.setFlowRate((double) currentFlow / maxCapacity);

                        // 趋势判断
                        double rate = flow.getFlowRate();
                        if (rate > 0.7) {
                            flow.setTrend("up");
                        } else if (rate < 0.4) {
                            flow.setTrend("down");
                        } else {
                            flow.setTrend("stable");
                        }

                        return flow;
                    })
                    .collect(Collectors.toList());

            log.info("成功获取{}个景区的流量数据", flowData.size());
            return Result.success(flowData);
        } catch (Exception e) {
            log.error("获取景区流量数据失败", e);
            return Result.error("获取景区流量数据失败");
        }
    }

    /**
     * 根据历史数据估算当前流量
     */
    private int estimateCurrentFlow(ScenicSpot spot, LocalDate today, int maxCapacity) {
        if (scenicStatisticsMapper == null) {
            return (int) (maxCapacity * 0.3);
        }

        try {
            // 查询当天的统计数据
            ScenicStatistics todayStats = scenicStatisticsMapper.selectOne(
                    new LambdaQueryWrapper<ScenicStatistics>()
                            .eq(ScenicStatistics::getScenicId, spot.getId())
                            .eq(ScenicStatistics::getStatDate, today));

            if (todayStats != null && todayStats.getVisitorCount() != null) {
                int flow = (int) (todayStats.getVisitorCount() * 0.1);
                log.debug("景区 {} 使用当天统计数据估算: {}", spot.getName(), flow);
                return flow;
            }

            // 查询最近7天的平均值
            LocalDate weekAgo = today.minusDays(7);
            List<ScenicStatistics> recentStats = scenicStatisticsMapper.selectList(
                    new LambdaQueryWrapper<ScenicStatistics>()
                            .eq(ScenicStatistics::getScenicId, spot.getId())
                            .ge(ScenicStatistics::getStatDate, weekAgo)
                            .lt(ScenicStatistics::getStatDate, today)
                            .orderByDesc(ScenicStatistics::getStatDate));

            if (recentStats != null && !recentStats.isEmpty()) {
                double avgFlow = recentStats.stream()
                        .filter(s -> s.getVisitorCount() != null)
                        .mapToInt(ScenicStatistics::getVisitorCount)
                        .average()
                        .orElse(0);
                int flow = (int) (avgFlow * 0.1);
                log.debug("景区 {} 使用最近7天平均数据估算: {}", spot.getName(), flow);
                return flow;
            }

            log.debug("景区 {} 无历史数据，使用估算值", spot.getName());
            return (int) (maxCapacity * 0.3);
        } catch (Exception e) {
            log.error("获取景区 {} 历史数据失败: {}", spot.getName(), e.getMessage());
            return (int) (maxCapacity * 0.3);
        }
    }

    /**
     * 获取旅游资讯
     * GET /dashboard/news?limit=10
     */
    @GetMapping("/news")
    public Result<List<TourismNews>> getNews(@RequestParam(defaultValue = "10") int limit) {
        log.info("获取旅游资讯: limit={}", limit);

        try {
            if (newsMapper == null) {
                log.warn("NewsMapper未注入，返回空列表");
                return Result.success(new ArrayList<>());
            }

            List<News> newsList = newsMapper.selectList(
                    new LambdaQueryWrapper<News>()
                            .eq(News::getStatus, "published")
                            .orderByDesc(News::getPublishTime)
                            .last("LIMIT " + limit));

            List<TourismNews> tourismNews = newsList.stream()
                    .map(news -> {
                        TourismNews tn = new TourismNews();
                        tn.setId(news.getId());
                        tn.setTitle(news.getTitle());
                        tn.setContent(news.getSummary() != null ? news.getSummary() : news.getContent());
                        tn.setPublishTime(news.getPublishTime() != null ? news.getPublishTime().toString() : null);

                        if ("notice".equalsIgnoreCase(news.getCategory())) {
                            tn.setCategory("notice");
                        } else if ("event".equalsIgnoreCase(news.getCategory())) {
                            tn.setCategory("event");
                        } else {
                            tn.setCategory("promotion");
                        }

                        tn.setImportant(false);
                        return tn;
                    })
                    .collect(Collectors.toList());

            return Result.success(tourismNews);
        } catch (Exception e) {
            log.error("获取旅游资讯失败", e);
            return Result.success(new ArrayList<>());
        }
    }

    /**
     * 获取用户统计数据
     * GET /dashboard/user-stats
     */
    @GetMapping("/user-stats")
    public Result<UserStats> getUserStats(
            @RequestAttribute(value = "userId", required = false) Long userId) {

        if (userId == null) {
            return Result.error("未登录");
        }

        log.info("获取用户统计数据: userId={}", userId);

        try {
            UserStats stats = new UserStats();
            stats.setTotalVisits(10);

            int favoriteCount = 0;
            if (userFavoriteService != null) {
                Long count = userFavoriteService.count(
                        new LambdaQueryWrapper<UserFavorite>()
                                .eq(UserFavorite::getUserId, userId));
                favoriteCount = count != null ? count.intValue() : 0;
            }
            stats.setFavoriteSpots(favoriteCount);
            stats.setSavedPlans(2);

            if (scenicReviewMapper != null) {
                Long reviewCount = scenicReviewMapper.selectCount(
                        new LambdaQueryWrapper<ScenicReview>()
                                .eq(ScenicReview::getUserId, userId));
                stats.setReviewsCount(reviewCount.intValue());
            } else {
                stats.setReviewsCount(0);
            }

            return Result.success(stats);
        } catch (Exception e) {
            log.error("获取用户统计数据失败", e);
            UserStats defaultStats = new UserStats();
            defaultStats.setTotalVisits(0);
            defaultStats.setSavedPlans(0);
            defaultStats.setFavoriteSpots(0);
            defaultStats.setReviewsCount(0);
            return Result.success(defaultStats);
        }
    }

    /**
     * 获取热门景点
     * GET /dashboard/popular-spots?limit=5
     */
    @GetMapping("/popular-spots")
    public Result<List<PopularSpot>> getPopularSpots(@RequestParam(defaultValue = "5") int limit) {
        log.info("获取热门景点: limit={}", limit);
        try {
            if (scenicSpotService == null) {
                return Result.success(new ArrayList<>());
            }

            List<ScenicSpot> spots = scenicSpotService.getAllActive();

            List<PopularSpot> popularSpots = spots.stream()
                    .map(spot -> {
                        PopularSpot ps = new PopularSpot();
                        ps.setId(spot.getId());
                        ps.setName(spot.getName());

                        // 优先从 scenic_images 表获取封面图
                        String imageUrl = null;
                        if (scenicImageService != null) {
                            try {
                                List<ScenicImage> coverImages =
                                    scenicImageService.getByScenicIdAndType(spot.getId(), "COVER");
                                if (coverImages != null && !coverImages.isEmpty()) {
                                    imageUrl = coverImages.get(0).getImageUrl();
                                } else {
                                    List<ScenicImage> allImages =
                                        scenicImageService.getByScenicId(spot.getId());
                                    if (allImages != null && !allImages.isEmpty()) {
                                        imageUrl = allImages.get(0).getImageUrl();
                                    }
                                }
                            } catch (Exception e) {
                                log.warn("获取景区[{}]图片失败: {}", spot.getName(), e.getMessage());
                            }
                        }

                        if (imageUrl == null || imageUrl.isEmpty()) {
                            imageUrl = spot.getImageUrl();
                        }

                        ps.setImage(imageUrl != null && !imageUrl.isEmpty() ? imageUrl : "/images/placeholder.jpg");
                        ps.setDescription(spot.getDescription());
                        ps.setRating(spot.getRating() != null ? spot.getRating().doubleValue() : 4.5);
                        ps.setCategory(spot.getLevel());

                        int favoritesCount = 0;
                        if (userFavoriteService != null) {
                            Long count = userFavoriteService.count(
                                    new LambdaQueryWrapper<UserFavorite>()
                                            .eq(UserFavorite::getScenicId, spot.getId()));
                            favoritesCount = count != null ? count.intValue() : 0;
                        }
                        ps.setFavoritesCount(favoritesCount);

                        return ps;
                    })
                    .sorted((s1, s2) -> {
                        int score1 = (int) (s1.getRating() * 10) + s1.getFavoritesCount();
                        int score2 = (int) (s2.getRating() * 10) + s2.getFavoritesCount();
                        return score2 - score1;
                    })
                    .limit(limit)
                    .collect(Collectors.toList());

            return Result.success(popularSpots);
        } catch (Exception e) {
            log.error("获取热门景点失败", e);
            return Result.error("获取热门景点失败");
        }
    }

    /**
     * 获取天气信息
     * GET /dashboard/weather?city=六盘水
     * 使用高德天气API获取实时天气
     */
    @GetMapping("/weather")
    public Result<WeatherInfo> getWeather(@RequestParam(defaultValue = "六盘水") String city) {
        log.info("获取天气信息: city={}", city);

        WeatherInfo weather = new WeatherInfo();
        weather.setCity(city);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try {
            // 高德天气API（城市编码和API Key从配置文件读取）
            String apiUrl = "https://restapi.amap.com/v3/weather/weatherInfo?city=" + amapCityCode + "&key=" + amapWeatherKey + "&extensions=all";

            RestTemplate restTemplate = new RestTemplate();

            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);

                if (response != null && "1".equals(response.get("status"))) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> forecasts = (List<Map<String, Object>>) response.get("forecasts");

                    if (forecasts != null && !forecasts.isEmpty()) {
                        Map<String, Object> forecast = forecasts.get(0);
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> casts = (List<Map<String, Object>>) forecast.get("casts");

                        if (casts != null && !casts.isEmpty()) {
                            Map<String, Object> today = casts.get(0);

                            double dayTemp = Double.parseDouble((String) today.get("daytemp"));
                            double nightTemp = Double.parseDouble((String) today.get("nighttemp"));

                            int hour = LocalTime.now().getHour();
                            double currentTemp = (hour >= 6 && hour < 18)
                                    ? nightTemp + (dayTemp - nightTemp) * 0.7
                                    : nightTemp + (dayTemp - nightTemp) * 0.3;

                            weather.setTemperature(Math.round(currentTemp * 10.0) / 10.0);
                            weather.setMinTemp(nightTemp);
                            weather.setMaxTemp(dayTemp);
                            weather.setCondition((String) today.get("dayweather"));

                            String weatherCondition = (String) today.get("dayweather");
                            // 湿度：高德API不返回湿度，按天气类型给出固定估计值
                            if (weatherCondition != null) {
                                if (weatherCondition.contains("雨")) {
                                    weather.setHumidity(85.0);
                                } else if (weatherCondition.contains("阴") || weatherCondition.contains("云")) {
                                    weather.setHumidity(70.0);
                                } else {
                                    weather.setHumidity(55.0);
                                }
                            } else {
                                weather.setHumidity(60.0);
                            }

                            weather.setUpdateTime(LocalDateTime.now().format(formatter));
                            log.info("成功获取高德天气数据: {}", weather);
                            return Result.success(weather);
                        }
                    }
                }
            } catch (Exception apiEx) {
                log.warn("高德天气API调用失败，尝试备用方案: {}", apiEx.getMessage());
            }

            // 备用方案：wttr.in
            try {
                String wttrUrl = "https://wttr.in/" + URLEncoder.encode(city, "UTF-8") + "?format=j1";
                @SuppressWarnings("unchecked")
                Map<String, Object> wttrResponse = restTemplate.getForObject(wttrUrl, Map.class);

                if (wttrResponse != null) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> currentCondition =
                        (List<Map<String, Object>>) wttrResponse.get("current_condition");

                    if (currentCondition != null && !currentCondition.isEmpty()) {
                        Map<String, Object> current = currentCondition.get(0);

                        weather.setTemperature(Double.parseDouble((String) current.get("temp_C")));
                        weather.setHumidity(Double.parseDouble((String) current.get("humidity")));

                        @SuppressWarnings("unchecked")
                        List<Map<String, String>> weatherDesc =
                            (List<Map<String, String>>) current.get("lang_zh");
                        if (weatherDesc != null && !weatherDesc.isEmpty()) {
                            weather.setCondition(weatherDesc.get(0).get("value"));
                        } else {
                            @SuppressWarnings("unchecked")
                            List<Map<String, String>> weatherDescEn =
                                (List<Map<String, String>>) current.get("weatherDesc");
                            if (weatherDescEn != null && !weatherDescEn.isEmpty()) {
                                weather.setCondition(weatherDescEn.get(0).get("value"));
                            }
                        }

                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> weatherList =
                            (List<Map<String, Object>>) wttrResponse.get("weather");
                        if (weatherList != null && !weatherList.isEmpty()) {
                            Map<String, Object> todayWeather = weatherList.get(0);
                            weather.setMinTemp(Double.parseDouble((String) todayWeather.get("mintempC")));
                            weather.setMaxTemp(Double.parseDouble((String) todayWeather.get("maxtempC")));
                        }

                        weather.setUpdateTime(LocalDateTime.now().format(formatter));
                        log.info("成功获取wttr.in天气数据: {}", weather);
                        return Result.success(weather);
                    }
                }
            } catch (Exception wttrEx) {
                log.warn("wttr.in天气API调用失败: {}", wttrEx.getMessage());
            }

        } catch (Exception e) {
            log.error("获取天气信息失败", e);
        }

        // 所有API都失败时，返回基于时间的模拟数据
        log.warn("所有天气API都失败，使用模拟数据");
        fillMockWeather(weather, formatter);
        return Result.success(weather);
    }

    /**
     * 填充模拟天气数据（当所有API失败时）
     */
    private void fillMockWeather(WeatherInfo weather, DateTimeFormatter formatter) {
        int hour = LocalTime.now().getHour();
        int month = LocalDate.now().getMonthValue();

        // 根据月份调整基础温度（六盘水气候凉爽）
        double baseTemp;
        if (month >= 6 && month <= 8) {
            baseTemp = 20;
        } else if (month >= 12 || month <= 2) {
            baseTemp = 8;
        } else if (month >= 3 && month <= 5) {
            baseTemp = 15;
        } else {
            baseTemp = 16;
        }

        double tempVariation;
        if (hour >= 6 && hour < 10) {
            tempVariation = -2;
        } else if (hour >= 10 && hour < 14) {
            tempVariation = 4;
        } else if (hour >= 14 && hour < 18) {
            tempVariation = 2;
        } else {
            tempVariation = -3;
        }

        weather.setTemperature(baseTemp + tempVariation);
        weather.setMinTemp(baseTemp - 4);
        weather.setMaxTemp(baseTemp + 6);
        weather.setCondition("晴朗（离线估算）");
        weather.setHumidity(55.0);
        weather.setUpdateTime(LocalDateTime.now().format(formatter));
    }

}
