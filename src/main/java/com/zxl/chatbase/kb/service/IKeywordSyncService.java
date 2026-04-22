package com.zxl.chatbase.kb.service;

public interface IKeywordSyncService {

    void syncKeywordsFromMessages();

    void cleanOldKeywords();
}