package com.zxl.chatbase.controller;

import com.zxl.chatbase.kb.entity.SysUser;
import com.zxl.chatbase.user.dto.LoginRequest;
import com.zxl.chatbase.user.dto.LoginResponse;
import com.zxl.chatbase.user.dto.RegisterRequest;
import com.zxl.chatbase.user.dto.UserVO;
import com.zxl.chatbase.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
    public UserVO getCurrentUser(@RequestParam String username) {
        return userService.getCurrentUser(username);
    }

    @PutMapping("/info")
    public Map<String, Object> updateUser(
            @RequestParam String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String avatar,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone) {
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

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "登出成功");
        return result;
    }

    @PostMapping("/check-password")
    public Map<String, Object> checkPassword(
            @RequestParam String username,
            @RequestParam String password) {
        boolean valid = userService.checkPassword(username, password);
        Map<String, Object> result = new HashMap<>();
        result.put("valid", valid);
        return result;
    }
}