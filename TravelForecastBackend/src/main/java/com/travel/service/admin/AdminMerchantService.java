package com.travel.service.admin;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.travel.dto.admin.MerchantStats;
import com.travel.entity.user.User;
import com.travel.dto.admin.MerchantListItem;

/**
 * 管理员-商家审核服务
 */
public interface AdminMerchantService {

    /**
     * 分页查询待审核商家（role=merchant, status=pending）
     */
    Page<User> pagePendingMerchants(long page, long size);

    /**
     * 审核通过：设置 status=active
     */
    void approve(Long userId, Long adminUserId, String reason);

    /**
     * 审核拒绝：设置 status=inactive
     */
    void reject(Long userId, Long adminUserId, String reason);

    /**
     * 审核必填检查：商家资料必须完整（名称、类型、联系人、电话、营业执照号）
     */
    void assertProfileRequiredFilled(Long userId);

    /**
     * 获取商家统计数据
     */
    MerchantStats stats();

    /**
     * 商户综合列表
     */
    Page<MerchantListItem> list(long page, long size, String keyword);
}


