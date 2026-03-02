package com.travel.service.scenic.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travel.entity.scenic.ScenicVideo;
import com.travel.mapper.scenic.ScenicVideoMapper;
import com.travel.service.scenic.ScenicVideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 景区视频服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenicVideoServiceImpl implements ScenicVideoService {

    private final ScenicVideoMapper scenicVideoMapper;

    @Override
    public List<ScenicVideo> getByScenicId(Long scenicId) {
        return scenicVideoMapper.selectByScenicId(scenicId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScenicVideo add(ScenicVideo scenicVideo) {
        scenicVideo.setCreatedAt(LocalDateTime.now());
        scenicVideoMapper.insert(scenicVideo);
        log.info("添加景区视频成功: scenicId={}, title={}", scenicVideo.getScenicId(), scenicVideo.getTitle());
        return scenicVideo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchAdd(List<ScenicVideo> videos) {
        for (ScenicVideo video : videos) {
            video.setCreatedAt(LocalDateTime.now());
            scenicVideoMapper.insert(video);
        }
        log.info("批量添加景区视频成功: count={}", videos.size());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        int result = scenicVideoMapper.deleteById(id);
        log.info("删除景区视频: {}, 结果: {}", id, result > 0);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByScenicId(Long scenicId) {
        LambdaQueryWrapper<ScenicVideo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScenicVideo::getScenicId, scenicId);
        int result = scenicVideoMapper.delete(wrapper);
        log.info("删除景区所有视频: scenicId={}, count={}", scenicId, result);
        return result > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(ScenicVideo scenicVideo) {
        int result = scenicVideoMapper.updateById(scenicVideo);
        log.info("更新景区视频: {}, 结果: {}", scenicVideo.getId(), result > 0);
        return result > 0;
    }
}

