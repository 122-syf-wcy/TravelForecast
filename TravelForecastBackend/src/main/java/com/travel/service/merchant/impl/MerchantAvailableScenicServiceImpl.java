package com.travel.service.merchant.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.travel.entity.merchant.MerchantAvailableScenic;
import com.travel.mapper.merchant.MerchantAvailableScenicMapper;
import com.travel.service.merchant.MerchantAvailableScenicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 商家可申请景区服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantAvailableScenicServiceImpl implements MerchantAvailableScenicService {
    
    private final MerchantAvailableScenicMapper mapper;
    
    @Override
    public List<MerchantAvailableScenic> getAvailableList() {
        QueryWrapper<MerchantAvailableScenic> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_available", true);
        queryWrapper.orderByDesc("priority");
        queryWrapper.orderByAsc("scenic_id");
        return mapper.selectList(queryWrapper);
    }
    
    @Override
    public List<MerchantAvailableScenic> getAllList() {
        QueryWrapper<MerchantAvailableScenic> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("is_available");
        queryWrapper.orderByDesc("priority");
        queryWrapper.orderByAsc("scenic_id");
        return mapper.selectList(queryWrapper);
    }
    
    @Override
    public MerchantAvailableScenic add(Long scenicId, Integer maxMerchants) {
        // 检查是否已存在
        QueryWrapper<MerchantAvailableScenic> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("scenic_id", scenicId);
        MerchantAvailableScenic existing = mapper.selectOne(queryWrapper);
        
        if (existing != null) {
            log.warn("景区 {} 已在可申请列表中", scenicId);
            return existing;
        }
        
        MerchantAvailableScenic scenic = new MerchantAvailableScenic();
        scenic.setScenicId(scenicId);
        scenic.setIsAvailable(true);
        scenic.setMaxMerchants(maxMerchants != null ? maxMerchants : 1);
        scenic.setCurrentMerchants(0);
        scenic.setPriority(0);
        
        mapper.insert(scenic);
        log.info("添加可申请景区: scenicId={}, maxMerchants={}", scenicId, maxMerchants);
        return scenic;
    }
    
    @Override
    public boolean update(Long id, Boolean isAvailable, Integer maxMerchants, Integer priority) {
        UpdateWrapper<MerchantAvailableScenic> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", id);
        
        if (isAvailable != null) {
            updateWrapper.set("is_available", isAvailable);
        }
        if (maxMerchants != null) {
            updateWrapper.set("max_merchants", maxMerchants);
        }
        if (priority != null) {
            updateWrapper.set("priority", priority);
        }
        
        int rows = mapper.update(null, updateWrapper);
        log.info("更新可申请景区配置: id={}, rows={}", id, rows);
        return rows > 0;
    }
    
    @Override
    public boolean delete(Long id) {
        int rows = mapper.deleteById(id);
        log.info("删除可申请景区: id={}, rows={}", id, rows);
        return rows > 0;
    }
    
    @Override
    public boolean isFull(Long scenicId) {
        QueryWrapper<MerchantAvailableScenic> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("scenic_id", scenicId);
        MerchantAvailableScenic scenic = mapper.selectOne(queryWrapper);
        
        if (scenic == null || !scenic.getIsAvailable()) {
            return true; // 不存在或不可用，视为已满
        }
        
        return scenic.getCurrentMerchants() >= scenic.getMaxMerchants();
    }
    
    @Override
    public boolean incrementMerchantCount(Long scenicId) {
        UpdateWrapper<MerchantAvailableScenic> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("scenic_id", scenicId);
        updateWrapper.setSql("current_merchants = current_merchants + 1");
        
        int rows = mapper.update(null, updateWrapper);
        log.info("增加景区商家数量: scenicId={}, rows={}", scenicId, rows);
        return rows > 0;
    }
    
    @Override
    public boolean decrementMerchantCount(Long scenicId) {
        UpdateWrapper<MerchantAvailableScenic> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("scenic_id", scenicId);
        updateWrapper.setSql("current_merchants = GREATEST(current_merchants - 1, 0)");
        
        int rows = mapper.update(null, updateWrapper);
        log.info("减少景区商家数量: scenicId={}, rows={}", scenicId, rows);
        return rows > 0;
    }
}

