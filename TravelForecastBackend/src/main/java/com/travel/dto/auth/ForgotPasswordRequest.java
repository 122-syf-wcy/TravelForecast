package com.travel.dto.auth;

import lombok.Data;

@Data
public class ForgotPasswordRequest {
    private String usernameOrEmail;
}


