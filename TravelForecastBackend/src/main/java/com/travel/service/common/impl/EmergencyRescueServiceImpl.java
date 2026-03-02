package com.travel.service.common.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.travel.entity.common.EmergencyRescue;
import com.travel.entity.merchant.MerchantProfile;
import com.travel.mapper.common.EmergencyRescueMapper;
import com.travel.mapper.merchant.MerchantProfileMapper;
import com.travel.service.common.EmergencyRescueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 紧急救援服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmergencyRescueServiceImpl extends ServiceImpl<EmergencyRescueMapper, EmergencyRescue> 
        implements EmergencyRescueService {

    private final EmergencyRescueMapper emergencyRescueMapper;
    private final MerchantProfileMapper merchantProfileMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createRescue(EmergencyRescue rescue) {
        // 设置默认状态
        if (rescue.getStatus() == null) {
            rescue.setStatus("PENDING");
        }
        if (rescue.getEmergencyLevel() == null) {
            rescue.setEmergencyLevel("NORMAL");
        }
        return this.save(rescue);
    }

    @Override
    public List<Map<String, Object>> getRescueListByScenicId(Long scenicId) {
        return emergencyRescueMapper.selectByScenicId(scenicId);
    }

    @Override
    public List<Map<String, Object>> getRescueListByUserId(Long userId) {
        return emergencyRescueMapper.selectByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean handleRescue(Long rescueId, Long handlerId, String handlerName) {
        EmergencyRescue rescue = this.getById(rescueId);
        if (rescue == null) {
            throw new RuntimeException("救援记录不存在");
        }
        if (!"PENDING".equals(rescue.getStatus())) {
            throw new RuntimeException("救援请求状态不正确，无法处理");
        }
        
        rescue.setStatus("PROCESSING");
        rescue.setHandlerId(handlerId);
        rescue.setHandlerName(handlerName);
        rescue.setHandleTime(LocalDateTime.now());
        
        return this.updateById(rescue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean completeRescue(Long rescueId, String handleNotes) {
        EmergencyRescue rescue = this.getById(rescueId);
        if (rescue == null) {
            throw new RuntimeException("救援记录不存在");
        }
        if (!"PROCESSING".equals(rescue.getStatus())) {
            throw new RuntimeException("救援请求状态不正确，无法完成");
        }
        
        rescue.setStatus("COMPLETED");
        rescue.setHandleNotes(handleNotes);
        rescue.setCompleteTime(LocalDateTime.now());
        
        return this.updateById(rescue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelRescue(Long rescueId) {
        EmergencyRescue rescue = this.getById(rescueId);
        if (rescue == null) {
            throw new RuntimeException("救援记录不存在");
        }
        
        rescue.setStatus("CANCELLED");
        return this.updateById(rescue);
    }

    @Override
    public Map<String, Object> getStatsByScenicId(Long scenicId) {
        return emergencyRescueMapper.getStatsByScenicId(scenicId);
    }

    @Override
    public List<Map<String, Object>> getRescueListByMerchantUserId(Long merchantUserId) {
        log.info("========== 开始查询商家救援数据 ==========");
        log.info("商家用户ID: {}", merchantUserId);
        
        try {
            // 先根据商家用户ID查询其管理的景区
            LambdaQueryWrapper<MerchantProfile> merchantWrapper = new LambdaQueryWrapper<>();
            merchantWrapper.eq(MerchantProfile::getUserId, merchantUserId)
                          .isNotNull(MerchantProfile::getScenicId);
            
            List<MerchantProfile> profiles = merchantProfileMapper.selectList(merchantWrapper);
            log.info("查询到商家绑定的景区数量: {}", profiles.size());
            
            if (profiles.isEmpty()) {
                log.warn("商家 {} 没有绑定任何景区，返回空列表", merchantUserId);
                return new ArrayList<>();
            }
            
            // 获取该商家管理的所有景区的救援记录
            List<Map<String, Object>> allRescues = new ArrayList<>();
            for (MerchantProfile profile : profiles) {
                log.info("查询景区 {} 的救援数据", profile.getScenicId());
                List<Map<String, Object>> rescues = emergencyRescueMapper.selectByScenicId(profile.getScenicId());
                log.info("景区 {} 有 {} 条救援记录", profile.getScenicId(), rescues.size());
                allRescues.addAll(rescues);
            }
            
            log.info("商家 {} 总共查询到 {} 条救援记录", merchantUserId, allRescues.size());
            log.info("========== 查询完成 ==========");
            return allRescues;
        } catch (Exception e) {
            log.error("查询商家救援数据失败", e);
            throw e;
        }
    }
}

