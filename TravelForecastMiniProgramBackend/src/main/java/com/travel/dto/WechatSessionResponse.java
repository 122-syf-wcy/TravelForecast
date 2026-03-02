package com.travel.dto;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

@Data
public class WechatSessionResponse {
    private String openid;
    
    @JSONField(name = "session_key")
    private String sessionKey;
    
    private String unionid;
    private Integer errcode;
    private String errmsg;

    public boolean isSuccess() {
        return this.errcode == null || this.errcode == 0;
    }
}
