package com.travel.service.merchant.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.merchant.MerchantContract;
import com.travel.mapper.merchant.MerchantContractMapper;
import com.travel.service.merchant.MerchantContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantContractServiceImpl implements MerchantContractService {

    private final MerchantContractMapper contractMapper;

    @Override
    @Transactional
    public MerchantContract uploadContract(Long userId, String contractNo, String contractName, String contractUrl, LocalDate startDate, LocalDate endDate, String remarks) {
        MerchantContract contract = new MerchantContract();
        contract.setUserId(userId);
        contract.setContractNo(contractNo);
        contract.setContractName(contractName);
        contract.setContractUrl(contractUrl);
        contract.setStartDate(startDate);
        contract.setEndDate(endDate);
        contract.setStatus("active");
        contract.setRemarks(remarks);
        contract.setCreatedAt(LocalDateTime.now());
        contract.setUpdatedAt(LocalDateTime.now());
        contractMapper.insert(contract);
        log.info("用户 {} 上传了新合同: {}", userId, contractNo);
        return contract;
    }

    @Override
    @Transactional
    public MerchantContract updateContract(Long contractId, String contractNo, String contractName, String contractUrl, LocalDate startDate, LocalDate endDate, String remarks, String status) {
        MerchantContract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            throw new RuntimeException("合同不存在");
        }
        contract.setContractNo(contractNo);
        contract.setContractName(contractName);
        contract.setContractUrl(contractUrl);
        contract.setStartDate(startDate);
        contract.setEndDate(endDate);
        contract.setStatus(status);
        contract.setRemarks(remarks);
        contract.setUpdatedAt(LocalDateTime.now());
        contractMapper.updateById(contract);
        log.info("更新合同 {}: {}", contractId, contractNo);
        return contract;
    }

    @Override
    @Transactional
    public boolean deleteContract(Long contractId) {
        int result = contractMapper.deleteById(contractId);
        if (result > 0) {
            log.info("删除合同: {}", contractId);
            return true;
        }
        return false;
    }

    @Override
    public MerchantContract getContractById(Long contractId) {
        return contractMapper.selectById(contractId);
    }

    @Override
    public List<MerchantContract> getContractsByUserId(Long userId) {
        LambdaQueryWrapper<MerchantContract> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MerchantContract::getUserId, userId);
        wrapper.orderByDesc(MerchantContract::getCreatedAt);
        return contractMapper.selectList(wrapper);
    }

    @Override
    public Page<MerchantContract> getAdminContractPage(int pageNum, int pageSize, String keyword, String status) {
        Page<MerchantContract> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<MerchantContract> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(MerchantContract::getContractNo, keyword)
                    .or().like(MerchantContract::getContractName, keyword);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(MerchantContract::getStatus, status);
        }
        wrapper.orderByDesc(MerchantContract::getCreatedAt);
        return contractMapper.selectPage(page, wrapper);
    }

    @Override
    @Transactional
    public boolean updateContractStatus(Long contractId, String status) {
        MerchantContract contract = contractMapper.selectById(contractId);
        if (contract == null) {
            return false;
        }
        contract.setStatus(status);
        contract.setUpdatedAt(LocalDateTime.now());
        int result = contractMapper.updateById(contract);
        log.info("更新合同 {} 状态为: {}", contractId, status);
        return result > 0;
    }
}

