package com.travel.dto.admin;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MerchantListItem {
    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String avatar;
    private String status; // active/pending/inactive

    private Long profileId;
    private String businessName;
    private String category;
    private String contact;
    private String address;
    private LocalDateTime createdAt;
}


