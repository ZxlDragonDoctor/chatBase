package com.zxl.chatbase.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zxl.chatbase.im.entity.ImGroup;
import com.zxl.chatbase.im.mapper.ImGroupMapper;
import com.zxl.chatbase.im.service.ImGroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImGroupServiceImpl implements ImGroupService {

    private final ImGroupMapper imGroupMapper;

    @Override
    public ImGroup getOrCreateGroup(String platform, String groupId, String groupName) {
        LambdaQueryWrapper<ImGroup> wrapper = new LambdaQueryWrapper<ImGroup>()
                .eq(ImGroup::getPlatform, platform)
                .eq(ImGroup::getGroupId, groupId);
        ImGroup group = imGroupMapper.selectOne(wrapper);
        if (group == null) {
            group = new ImGroup();
            group.setPlatform(platform);
            group.setGroupId(groupId);
            group.setGroupName(groupName);
            group.setStatus(true);
            group.setAutoReply(true);
            group.setMemberCount(0);
            group.setCreateTime(LocalDateTime.now());
            imGroupMapper.insert(group);
            log.info("新建群组: platform={}, groupId={}, groupName={}", platform, groupId, groupName);
        }
        return group;
    }

    @Override
    public void updateGroupInfo(ImGroup group) {
        if (group.getId() != null) {
            group.setUpdateTime(LocalDateTime.now());
            imGroupMapper.updateById(group);
        }
    }

    @Override
    public void bindApp(Long id, Long appId, String appName) {
        ImGroup group = imGroupMapper.selectById(id);
        if (group == null) {
            throw new RuntimeException("群组不存在");
        }
        group.setAppId(appId);
        group.setAppName(appName);
        group.setUpdateTime(LocalDateTime.now());
        imGroupMapper.updateById(group);
        log.info("群组绑定应用: groupId={}, appId={}, appName={}", id, appId, appName);
    }

    @Override
    public void unbindApp(Long id) {
        ImGroup group = imGroupMapper.selectById(id);
        if (group == null) {
            throw new RuntimeException("群组不存在");
        }
        group.setAppId(null);
        group.setAppName(null);
        group.setUpdateTime(LocalDateTime.now());
        imGroupMapper.updateById(group);
        log.info("群组解除应用绑定: groupId={}", id);
    }
}