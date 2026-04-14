package com.zxl.chatbase.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxl.chatbase.im.entity.ImUser;
import com.zxl.chatbase.im.mapper.ImUserMapper;
import com.zxl.chatbase.im.service.ImUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImUserServiceImpl implements ImUserService {

    private final ImUserMapper imUserMapper;

    @Override
    public ImUser getOrCreateUser(String platform, String userId, String groupId, String nickname) {
        LambdaQueryWrapper<ImUser> wrapper = new LambdaQueryWrapper<ImUser>()
                .eq(ImUser::getPlatform, platform)
                .eq(ImUser::getUserId, userId);
        ImUser user = imUserMapper.selectOne(wrapper);
        if (user == null) {
            user = new ImUser();
            user.setPlatform(platform);
            user.setUserId(userId);
            user.setNickname(nickname);
            user.setGroupId(groupId);
            user.setRole("member");
            user.setStatus(true);
            user.setMessageCount(0);
            user.setCreateTime(LocalDateTime.now());
            imUserMapper.insert(user);
            log.info("新建用户: platform={}, userId={}, nickname={}", platform, userId, nickname);
        } else {
            if (groupId != null && !groupId.equals(user.getGroupId())) {
                user.setGroupId(groupId);
            }
            if (nickname != null && !nickname.isEmpty()) {
                user.setNickname(nickname);
            }
            user.setLastMessageTime(LocalDateTime.now());
            user.setMessageCount(user.getMessageCount() + 1);
            imUserMapper.updateById(user);
        }
        return user;
    }

    @Override
    public void updateUserInfo(ImUser user) {
        if (user.getId() != null) {
            user.setUpdateTime(LocalDateTime.now());
            imUserMapper.updateById(user);
        }
    }
}