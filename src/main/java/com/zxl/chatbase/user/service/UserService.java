package com.zxl.chatbase.user.service;

import com.zxl.chatbase.user.dto.LoginRequest;
import com.zxl.chatbase.user.dto.LoginResponse;
import com.zxl.chatbase.user.dto.RegisterRequest;
import com.zxl.chatbase.user.dto.UserVO;
import com.zxl.chatbase.kb.entity.SysUser;

public interface UserService {
    
    SysUser register(RegisterRequest request);
    
    LoginResponse login(LoginRequest request);
    
    UserVO getCurrentUser(String username);
    
    UserVO updateUser(String username, String nickname, String avatar, String email, String phone);
    
    boolean checkPassword(String username, String rawPassword);
}