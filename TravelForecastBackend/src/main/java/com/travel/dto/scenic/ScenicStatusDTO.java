package com.travel.dto.scenic;

import lombok.Data;

/**
 * 景区运营状态DTO
 */
@Data
public class ScenicStatusDTO {
    
    /**
     * 景区ID
     */
    private Long scenicId;
    
    /**
     * 景区名称
     */
    private String scenicName;
    
    /**
     * 运营状态
     * open: 正常开放
     * busy: 繁忙
     * crowded: 拥挤
     * limited: 限流
     * closed: 关闭
     */
    private String status;
    
    /**
     * 状态描述
     */
    private String statusDescription;
    
    /**
     * 当前客流量
     */
    private Integer currentFlow;
    
    /**
     * 最大容量
     */
    private Integer maxCapacity;
    
    /**
     * 客流率（0-1）
     */
    private Double flowRate;
    
    /**
     * 预测客流量（未来1小时）
     */
    private Integer predictedFlow;
    
    /**
     * 预测客流率（未来1小时）
     */
    private Double predictedFlowRate;
    
    /**
     * 预测趋势
     * rising: 上升
     * stable: 稳定
     * falling: 下降
     */
    private String trend;
    
    /**
     * 建议等待时间（分钟）
     */
    private Integer suggestedWaitTime;
    
    /**
     * 是否建议现在前往
     */
    private Boolean recommended;
    
    /**
     * 建议说明
     */
    private String suggestion;
    
    /**
     * 更新时间
     */
    private String updateTime;
}

