package com.travel.service.merchant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.merchant.MerchantContract;

import java.time.LocalDate;
import java.util.List;

public interface MerchantContractService {
    MerchantContract uploadContract(Long userId, String contractNo, String contractName, String contractUrl, LocalDate startDate, LocalDate endDate, String remarks);
    MerchantContract updateContract(Long contractId, String contractNo, String contractName, String contractUrl, LocalDate startDate, LocalDate endDate, String remarks, String status);
    boolean deleteContract(Long contractId);
    MerchantContract getContractById(Long contractId);
    List<MerchantContract> getContractsByUserId(Long userId);
    Page<MerchantContract> getAdminContractPage(int pageNum, int pageSize, String keyword, String status);
    boolean updateContractStatus(Long contractId, String status);
}

