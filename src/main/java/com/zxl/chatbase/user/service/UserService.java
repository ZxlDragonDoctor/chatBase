package com.zxl.chatbase.user.service;

import com.zxl.chatbase.user.dto.LoginRequest;
import com.zxl.chatbase.user.dto.LoginResponse;
import com.zxl.chatbase.user.dto.PageResult;
import com.zxl.chatbase.user.dto.RegisterRequest;
import com.zxl.chatbase.user.dto.UserVO;
import com.zxl.chatbase.kb.entity.SysUser;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    
    SysUser register(RegisterRequest request);
    
    LoginResponse login(LoginRequest request);
    
    UserVO getCurrentUser(String username);
    
    UserVO updateUser(String username, String nickname, String avatar, String email, String phone);
    
    UserVO uploadAvatar(String username, MultipartFile file) throws Exception;
    
    boolean checkPassword(String username, String rawPassword);
    
    boolean changePassword(String username, String oldPassword, String newPassword);

    PageResult<UserVO> listUsers(int pageNum, int pageSize, String keyword);

    UserVO getUserDetail(Long id);

    UserVO updateUserRole(Long id, String role);

    UserVO toggleUserStatus(Long id, Boolean status);

    void deleteUser(Long id);
}