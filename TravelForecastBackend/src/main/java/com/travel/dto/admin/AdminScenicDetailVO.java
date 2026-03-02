package com.travel.dto.admin;

import com.travel.entity.scenic.ScenicImage;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.entity.scenic.ScenicVideo;
import lombok.Data;

import java.util.List;

/**
 * 管理员端景区详情VO
 */
@Data
public class AdminScenicDetailVO {
    private ScenicSpot scenic;
    private List<ScenicImage> images;
    private List<ScenicVideo> videos;
}

