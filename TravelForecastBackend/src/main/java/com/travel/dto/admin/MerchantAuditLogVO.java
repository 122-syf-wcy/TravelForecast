package com.travel.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MerchantAuditLogVO {
    private Long id;
    private Long userId;
    private Long adminUserId;
    private String adminUsername;
    private String adminNickname;
    private String action;
    private String remark;
    private String fromStatus;
    private String toStatus;
    private LocalDateTime createdAt;
}


