package com.travel.dto;

import lombok.Data;

/**
 * 微信小程序登录请求 DTO
 *
 * 支持两种获取用户信息的方式：
 * 1. 新版（推荐）：前端通过 chooseAvatar + nickname 组件收集，直接传 nickname/avatarUrl
 * 2. 旧版（兼容）：前端通过 wx.getUserProfile 获取 encryptedData/iv，后端解密
 */
@Data
public class WechatLoginRequest {

    /** 微信登录 code（必填，由 wx.login() 获取） */
    private String code;

    // ===== 新版：前端直接传递用户信息 =====

    /** 用户昵称（通过 <input type="nickname"> 获取） */
    private String nickname;

    /** 用户头像临时路径或 URL（通过 <button open-type="chooseAvatar"> 获取） */
    private String avatarUrl;

    // ===== 旧版：加密数据（兼容 wx.getUserProfile / wx.getUserInfo） =====

    /** 加密的用户数据 */
    private String encryptedData;

    /** 加密算法初始向量 */
    private String iv;

    /** 原始数据字符串 */
    private String rawData;

    /** 数据签名 */
    private String signature;
}
