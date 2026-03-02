package com.travel.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantStats {
    private long pending;
    private long approved;
    private long rejected;
    private long today;
}


