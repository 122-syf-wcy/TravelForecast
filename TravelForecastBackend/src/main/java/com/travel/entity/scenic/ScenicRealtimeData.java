package com.travel.entity.scenic;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 景区实时数据表
 */
@Data
@TableName("scenic_realtime_data")
public class ScenicRealtimeData implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long scenicId;

    private Integer visitorCount;

    private Integer crowdedness;

    private Integer waitTime;

    private Integer parkingAvailable;

    private LocalDateTime timestamp;
}

