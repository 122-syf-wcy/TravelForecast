package com.travel.dto.merchant;

import lombok.Data;

import java.time.LocalDate;

/**
 * 商家上传合同请求
 */
@Data
public class ContractUploadRequest {
    private String contractNo;
    private String contractName;
    private String contractUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private String remarks;
}

