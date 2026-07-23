package com.zxl.chatbase.user.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private String token;
    private UserVO user;
}