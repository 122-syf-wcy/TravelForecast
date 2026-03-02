package com.travel.service.merchant.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.merchant.MerchantProfile;
import com.travel.mapper.merchant.MerchantProfileMapper;
import com.travel.service.merchant.MerchantAvailableScenicService;
import com.travel.service.merchant.MerchantProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MerchantProfileServiceImpl implements MerchantProfileService {

    @Autowired
    private MerchantProfileMapper merchantProfileMapper;
    
    @Autowired
    private MerchantAvailableScenicService merchantAvailableScenicService;

    @Override
    public MerchantProfile getByUserId(Long userId) {
        return merchantProfileMapper.selectOne(new LambdaQueryWrapper<MerchantProfile>().eq(MerchantProfile::getUserId, userId));
    }

    @Override
    public MerchantProfile createEmptyForUser(Long userId) {
        MerchantProfile mp = new MerchantProfile();
        mp.setUserId(userId);
        merchantProfileMapper.insert(mp);
        return mp;
    }

    @Override
    public MerchantProfile saveOrUpdate(MerchantProfile profile) {
        Long oldScenicId = null;
        Long newScenicId = profile.getScenicId();
        
        // 先检查该用户是否已有商家资料
        MerchantProfile existingProfile = null;
        if (profile.getUserId() != null) {
            existingProfile = getByUserId(profile.getUserId());
        }
        
        if (existingProfile != null) {
            // 已存在，执行更新操作
            log.info("用户 {} 的商家资料已存在，执行更新操作", profile.getUserId());
            
            // 保留原有ID
            profile.setId(existingProfile.getId());
            oldScenicId = existingProfile.getScenicId();
            
            // 更新资料
            merchantProfileMapper.updateById(profile);
            
            // 如果景区ID发生变化，需要更新计数
            if (!java.util.Objects.equals(oldScenicId, newScenicId)) {
                // 如果原来有景区，减少原景区的商家数量
                if (oldScenicId != null) {
                    log.info("商家更换景区，减少旧景区 {} 的商家数量", oldScenicId);
                    merchantAvailableScenicService.decrementMerchantCount(oldScenicId);
                }
                
                // 如果现在有景区，增加新景区的商家数量
                if (newScenicId != null) {
                    log.info("商家更换景区，增加新景区 {} 的商家数量", newScenicId);
                    merchantAvailableScenicService.incrementMerchantCount(newScenicId);
                }
            }
        } else {
            // 不存在，执行新增操作
            log.info("用户 {} 首次创建商家资料", profile.getUserId());
            merchantProfileMapper.insert(profile);
            
            // 如果选择了景区，增加该景区的商家数量
            if (newScenicId != null) {
                log.info("新增商家，增加景区 {} 的商家数量", newScenicId);
                merchantAvailableScenicService.incrementMerchantCount(newScenicId);
            }
        }
        
        return profile;
    }

    @Override
    public Page<MerchantProfile> page(int pageNum, int pageSize, String keyword) {
        Page<MerchantProfile> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MerchantProfile> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(MerchantProfile::getBusinessName, keyword);
        }
        return merchantProfileMapper.selectPage(page, qw);
    }
}


