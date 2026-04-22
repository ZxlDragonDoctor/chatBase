package com.zxl.chatbase.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zxl.chatbase.config.TokenService;
import com.zxl.chatbase.kb.entity.SysUser;
import com.zxl.chatbase.kb.mapper.SysUserMapper;
import com.zxl.chatbase.user.dto.LoginRequest;
import com.zxl.chatbase.user.dto.LoginResponse;
import com.zxl.chatbase.user.dto.RegisterRequest;
import com.zxl.chatbase.user.dto.UserVO;
import com.zxl.chatbase.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final TokenService tokenService;

    @Override
    public SysUser register(RegisterRequest request) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername());
        if (getOne(wrapper) != null) {
            log.warn("用户名已存在: {}", request.getUsername());
            return null;
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setStatus(true);
        user.setRole("user");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setIsDeleted(false);

        save(user);
        log.info("用户注册成功: username={}, id={}", user.getUsername(), user.getId());
        return user;
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, request.getUsername());
        SysUser user = getOne(wrapper);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("登录失败: username={}", request.getUsername());
            return null;
        }

        if (!user.getStatus()) {
            log.warn("用户已被禁用: username={}", request.getUsername());
            return null;
        }

        user.setLastLoginTime(LocalDateTime.now());
        updateById(user);

        String token = tokenService.createToken(user.getUsername());

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUser(toUserVO(user));

        log.info("用户登录成功: username={}", user.getUsername());
        return response;
    }

    @Override
    public UserVO getCurrentUser(String username) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = getOne(wrapper);
        return user != null ? toUserVO(user) : null;
    }

    @Override
    public UserVO updateUser(String username, String nickname, String avatar, String email, String phone) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = getOne(wrapper);

        if (user == null) {
            return null;
        }

        if (nickname != null) user.setNickname(nickname);
        if (avatar != null) user.setAvatar(avatar);
        if (email != null) user.setEmail(email);
        if (phone != null) user.setPhone(phone);
        user.setUpdateTime(LocalDateTime.now());

        updateById(user);
        return toUserVO(user);
    }

    @Override
    public boolean checkPassword(String username, String rawPassword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = getOne(wrapper);
        return user != null && passwordEncoder.matches(rawPassword, user.getPassword());
    }

    private UserVO toUserVO(SysUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setRole(user.getRole());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}