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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements UserService {

    private static final String AVATAR_UPLOAD_DIR = "uploads/avatars";
    private static final List<String> ALLOWED_AVATAR_TYPES = Arrays.asList("jpg", "jpeg", "png", "gif", "webp");
    private static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5MB

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

    @Override
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = getOne(wrapper);
        
        if (user == null || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            log.warn("修改密码失败: 原密码错误, username={}", username);
            return false;
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);
        
        log.info("用户修改密码成功: username={}", username);
        return true;
    }

    @Override
    public UserVO uploadAvatar(String username, MultipartFile file) throws Exception {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = getOne(wrapper);
        
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        // Validate file type
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("无效的文件名");
        }
        String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_AVATAR_TYPES.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型，仅支持: " + String.join(", ", ALLOWED_AVATAR_TYPES));
        }

        // Validate file size
        if (file.getSize() > MAX_AVATAR_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过5MB");
        }

        // Create upload directory
        Path uploadDir = Paths.get(AVATAR_UPLOAD_DIR);
        if (!Files.exists(uploadDir)) {
            Files.createDirectories(uploadDir);
        }

        // Generate unique filename
        String newFilename = username + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
        Path targetPath = uploadDir.resolve(newFilename);

        // Save file
        Files.copy(file.getInputStream(), targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // Update user avatar URL
        String avatarUrl = "/uploads/avatars/" + newFilename;
        user.setAvatar(avatarUrl);
        user.setUpdateTime(LocalDateTime.now());
        updateById(user);

        log.info("用户头像上传成功: username={}, avatar={}", username, avatarUrl);
        return toUserVO(user);
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