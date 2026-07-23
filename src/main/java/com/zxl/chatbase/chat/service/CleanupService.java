package com.zxl.chatbase.chat.service;

public interface CleanupService {

    void cleanupExpiredConversations();

    void cleanupOldMessages();
}