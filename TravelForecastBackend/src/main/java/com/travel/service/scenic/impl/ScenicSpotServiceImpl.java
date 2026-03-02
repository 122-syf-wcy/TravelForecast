package com.travel.service.scenic.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.scenic.ScenicSpot;
import com.travel.mapper.scenic.ScenicSpotMapper;
import com.travel.service.scenic.ScenicSpotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 景区服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScenicSpotServiceImpl implements ScenicSpotService {

    private final ScenicSpotMapper scenicSpotMapper;

    @Override
    public Page<ScenicSpot> getPage(int page, int size, String status, String keyword) {
        Page<ScenicSpot> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ScenicSpot> wrapper = new LambdaQueryWrapper<>();

        // 默认只查主景区（parent_id IS NULL）
        wrapper.isNull(ScenicSpot::getParentId);

        if (status != null && !status.isEmpty()) {
            wrapper.eq(ScenicSpot::getStatus, status);
        }

        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(ScenicSpot::getName, keyword)
                    .or()
                    .like(ScenicSpot::getDescription, keyword)
                    .or()
                    .like(ScenicSpot::getCity, keyword));
        }

        wrapper.orderByDesc(ScenicSpot::getCreatedAt);
        return scenicSpotMapper.selectPage(pageParam, wrapper);
    }

    @Override
    public ScenicSpot getById(Long id) {
        return scenicSpotMapper.selectById(id);
    }

    @Override
    public ScenicSpot getBySpotCode(String spotCode) {
        LambdaQueryWrapper<ScenicSpot> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ScenicSpot::getSpotCode, spotCode);
        return scenicSpotMapper.selectOne(wrapper);
    }

    @Override
    public ScenicSpot getByIdOrCode(String idOrCode) {
        if (idOrCode == null || idOrCode.trim().isEmpty()) {
            return null;
        }
        
        // 尝试判断是ID还是编码
        // 如果是纯数字，尝试用ID查询
        try {
            Long id = Long.parseLong(idOrCode);
            ScenicSpot spot = getById(id);
            if (spot != null) {
                return spot;
            }
        } catch (NumberFormatException e) {
            // 不是数字，使用编码查询
        }
        
        // 使用编码查询
        return getBySpotCode(idOrCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScenicSpot create(ScenicSpot scenicSpot) {
        scenicSpot.setCreatedAt(LocalDateTime.now());
        scenicSpot.setUpdatedAt(LocalDateTime.now());
        if (scenicSpot.getStatus() == null) {
            scenicSpot.setStatus("ACTIVE");
        }
        scenicSpotMapper.insert(scenicSpot);
        log.info("创建景区成功: {}", scenicSpot.getName());
        return scenicSpot;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScenicSpot update(ScenicSpot scenicSpot) {
        scenicSpot.setUpdatedAt(LocalDateTime.now());
        scenicSpotMapper.updateById(scenicSpot);
        log.info("更新景区成功: {}", scenicSpot.getId());
        return scenicSpot;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        // 软删除
        LambdaUpdateWrapper<ScenicSpot> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ScenicSpot::getId, id)
                .set(ScenicSpot::getDeletedAt, LocalDateTime.now());
        int result = scenicSpotMapper.update(null, wrapper);
        log.info("删除景区: {}, 结果: {}", id, result > 0);
        return result > 0;
    }

    @Override
    public List<ScenicSpot> getAllActive() {
        return scenicSpotMapper.selectMainSpotsByStatus("ACTIVE");
    }

    @Override
    public List<ScenicSpot> getByCity(String city) {
        return scenicSpotMapper.selectByCity(city);
    }

    @Override
    public List<ScenicSpot> search(String keyword) {
        return scenicSpotMapper.searchByKeyword(keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(Long id, String status) {
        LambdaUpdateWrapper<ScenicSpot> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(ScenicSpot::getId, id)
                .set(ScenicSpot::getStatus, status)
                .set(ScenicSpot::getUpdatedAt, LocalDateTime.now());
        int result = scenicSpotMapper.update(null, wrapper);
        log.info("更新景区状态: {}, status: {}, 结果: {}", id, status, result > 0);
        return result > 0;
    }

    @Override
    public List<ScenicSpot> getSubSpots(Long parentId) {
        return scenicSpotMapper.selectByParentId(parentId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScenicSpot createSubSpot(Long parentId, ScenicSpot subSpot) {
        // 验证父景区存在
        ScenicSpot parent = scenicSpotMapper.selectById(parentId);
        if (parent == null) {
            throw new RuntimeException("父景区不存在");
        }
        subSpot.setParentId(parentId);
        subSpot.setCreatedAt(LocalDateTime.now());
        subSpot.setUpdatedAt(LocalDateTime.now());
        if (subSpot.getStatus() == null) {
            subSpot.setStatus("ACTIVE");
        }
        // 继承父景区的城市信息
        if (subSpot.getCity() == null) {
            subSpot.setCity(parent.getCity());
        }
        scenicSpotMapper.insert(subSpot);
        log.info("创建子景点成功: {} -> {}", parent.getName(), subSpot.getName());
        return subSpot;
    }
}

