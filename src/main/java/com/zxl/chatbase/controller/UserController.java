package com.zxl.chatbase.controller;

import com.zxl.chatbase.kb.entity.SysUser;
import com.zxl.chatbase.user.dto.LoginRequest;
import com.zxl.chatbase.user.dto.LoginResponse;
import com.zxl.chatbase.user.dto.RegisterRequest;
import com.zxl.chatbase.user.dto.UserVO;
import com.zxl.chatbase.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest request) {
        SysUser user = userService.register(request);
        Map<String, Object> result = new HashMap<>();
        if (user != null) {
            result.put("success", true);
            result.put("message", "注册成功");
            result.put("userId", user.getId());
        } else {
            result.put("success", false);
            result.put("message", "用户名已存在");
        }
        return result;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest request) {
        LoginResponse response = userService.login(request);
        Map<String, Object> result = new HashMap<>();
        if (response != null) {
            result.put("success", true);
            result.put("token", response.getToken());
            result.put("user", response.getUser());
        } else {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
        }
        return result;
    }

    @GetMapping("/info")
    public UserVO getCurrentUser(@RequestParam("username") String username) {
        return userService.getCurrentUser(username);
    }

    @PutMapping("/info")
    public Map<String, Object> updateUser(
            @RequestParam("username") String username,
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "avatar", required = false) String avatar,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone) {
        UserVO user = userService.updateUser(username, nickname, avatar, email, phone);
        Map<String, Object> result = new HashMap<>();
        if (user != null) {
            result.put("success", true);
            result.put("user", user);
        } else {
            result.put("success", false);
            result.put("message", "用户不存在");
        }
        return result;
    }

    @PostMapping(value = "/avatar/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> uploadAvatar(
            @RequestParam("username") String username,
            @RequestPart("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            UserVO user = userService.uploadAvatar(username, file);
            result.put("success", true);
            result.put("user", user);
            result.put("message", "头像上传成功");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "头像上传失败: " + e.getMessage());
        }
        return result;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "登出成功");
        return result;
    }

    @PostMapping("/change-password")
    public Map<String, Object> changePassword(
            @RequestParam("username") String username,
            @RequestParam("oldPassword") String oldPassword,
            @RequestParam("newPassword") String newPassword) {
        Map<String, Object> result = new HashMap<>();
        boolean success = userService.changePassword(username, oldPassword, newPassword);
        if (success) {
            result.put("success", true);
            result.put("message", "密码修改成功");
        } else {
            result.put("success", false);
            result.put("message", "原密码不正确");
        }
        return result;
    }

    @PostMapping("/check-password")
    public Map<String, Object> checkPassword(
            @RequestParam("username") String username,
            @RequestParam("password") String password) {
        boolean valid = userService.checkPassword(username, password);
        Map<String, Object> result = new HashMap<>();
        result.put("valid", valid);
        return result;
    }
}