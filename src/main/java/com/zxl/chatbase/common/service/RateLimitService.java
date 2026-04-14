package com.zxl.chatbase.common.service;

public interface RateLimitService {

    void checkRateLimit(String channel, String userId, String groupId);

    void recordRequest(String channel, String userId, String groupId);

    void reset(String channel, String userId, String groupId);
}