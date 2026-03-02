package com.travel.service.merchant;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.entity.merchant.MerchantProfile;

public interface MerchantProfileService {
    MerchantProfile getByUserId(Long userId);
    MerchantProfile createEmptyForUser(Long userId);
    MerchantProfile saveOrUpdate(MerchantProfile profile);
    Page<MerchantProfile> page(int pageNum, int pageSize, String keyword);
}


