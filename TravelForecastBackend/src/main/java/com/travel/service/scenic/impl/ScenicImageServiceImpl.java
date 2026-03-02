package com.travel.service.scenic.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.entity.scenic.ScenicImage;
import com.travel.mapper.scenic.ScenicImageMapper;
import com.travel.service.scenic.ScenicImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 景区图片服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenicImageServiceImpl implements ScenicImageService {

    private final ScenicImageMapper scenicImageMapper;

    @Override
    public ScenicImage getById(Long id) {
        return scenicImageMapper.selectById(id);
    }

    @Override
    public List<ScenicImage> getByScenicId(Long scenicId) {
        return scenicImageMapper.selectByScenicId(scenicId);
    }

    @Override
    public List<ScenicImage> getByScenicIdAndType(Long scenicId, String imageType) {
        return scenicImageMapper.selectByScenicIdAndType(scenicId, imageType);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScenicImage add(ScenicImage scenicImage) {
        scenicImage.setCreatedAt(LocalDateTime.now());
        scenicImageMapper.insert(scenicImage);
        log.info("添加景区图片成功: scenicId={}, type={}", scenicImage.getScenicId(), scenicImage.getImageType());
        return scenicImage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchAdd(List<ScenicImage> images) {
        for (ScenicImage image : images) {
            image.setCreatedAt(LocalDateTime.now());
            scenicImageMapper.insert(image);
        }
        log.info("批量添加景区图片成功: count={}", images.size());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        int result = scenicImageMapper.deleteById(id);
        log.info("删除景区图片: {}, 结果: {}", id, result > 0);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSortOrder(Long id, Integer sortOrder) {
        int result = scenicImageMapper.updateSortOrder(id, sortOrder);
        log.info("更新图片排序: {}, sortOrder: {}, 结果: {}", id, sortOrder, result > 0);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByScenicId(Long scenicId) {
        LambdaQueryWrapper<ScenicImage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScenicImage::getScenicId, scenicId);
        int result = scenicImageMapper.delete(wrapper);
        log.info("删除景区所有图片: scenicId={}, count={}", scenicId, result);
        return result > 0;
    }
}

