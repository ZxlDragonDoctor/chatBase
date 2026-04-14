package com.zxl.chatbase.im.service;

import com.zxl.chatbase.im.entity.ImGroup;

public interface ImGroupService {

    ImGroup getOrCreateGroup(String platform, String groupId, String groupName);

    void updateGroupInfo(ImGroup group);
}