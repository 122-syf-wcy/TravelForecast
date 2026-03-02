package com.travel.entity.scenic;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 景区统计数据实体类
 */
@TableName("scenic_statistics")
public class ScenicStatistics {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 景区ID
     */
    private Long scenicId;

    /**
     * 统计日期
     */
    private LocalDate statDate;

    /**
     * 游客数量
     */
    private Integer visitorCount;

    /**
     * 订单数量
     */
    private Integer orderCount;

    /**
     * 收入
     */
    private BigDecimal revenue;

    /**
     * 收藏数
     */
    private Integer favoritesCount;

    /**
     * 平均停留时间（小时）
     */
    private BigDecimal avgStayTime;

    /**
     * 天气
     */
    private String weather;

    /**
     * 是否周末
     */
    private Boolean isWeekend;

    /**
     * 是否节假日
     */
    private Boolean isHoliday;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getScenicId() {
        return scenicId;
    }

    public void setScenicId(Long scenicId) {
        this.scenicId = scenicId;
    }

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public Integer getVisitorCount() {
        return visitorCount;
    }

    public void setVisitorCount(Integer visitorCount) {
        this.visitorCount = visitorCount;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }

    public Integer getFavoritesCount() {
        return favoritesCount;
    }

    public void setFavoritesCount(Integer favoritesCount) {
        this.favoritesCount = favoritesCount;
    }

    public BigDecimal getAvgStayTime() {
        return avgStayTime;
    }

    public void setAvgStayTime(BigDecimal avgStayTime) {
        this.avgStayTime = avgStayTime;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public Boolean getIsWeekend() {
        return isWeekend;
    }

    public void setIsWeekend(Boolean isWeekend) {
        this.isWeekend = isWeekend;
    }

    public Boolean getIsHoliday() {
        return isHoliday;
    }

    public void setIsHoliday(Boolean isHoliday) {
        this.isHoliday = isHoliday;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
