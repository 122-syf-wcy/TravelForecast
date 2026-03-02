package com.travel.dto.admin;

import lombok.Data;

@Data
public class AuditDecisionRequest {
    private String reason; // 审核意见/拒绝原因（通过时可为空）
}


