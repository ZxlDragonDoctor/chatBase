package com.zxl.chatbase.im.service;

import com.zxl.chatbase.im.entity.ImUser;

public interface ImUserService {

    ImUser getOrCreateUser(String platform, String userId, String groupId, String nickname);

    void updateUserInfo(ImUser user);
}