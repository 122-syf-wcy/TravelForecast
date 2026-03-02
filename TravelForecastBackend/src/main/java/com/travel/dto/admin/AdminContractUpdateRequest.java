package com.travel.dto.admin;

import lombok.Data;

import java.time.LocalDate;

/**
 * 管理员更新合同请求
 */
@Data
public class AdminContractUpdateRequest {
    private String contractNo;
    private String contractName;
    private String contractUrl;
    private LocalDate startDate;
    private LocalDate endDate;
    private String remarks;
    private String status;
}

