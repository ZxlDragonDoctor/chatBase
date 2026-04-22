package com.zxl.chatbase.kb.service.impl;

import com.zxl.chatbase.kb.service.IKbKeywordService;
import com.zxl.chatbase.kb.service.IKeywordSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeywordSyncServiceImpl implements IKeywordSyncService {

    private final IKbKeywordService keywordService;

    @Value("${keyword.sync.limit:500}")
    private int syncLimit;

    @Value("${keyword.retention.days:90}")
    private int retentionDays;

    @Override
    @Scheduled(cron = "0 0 5 * * ?")
    public void syncKeywordsFromMessages() {
        log.info("开始同步关键词（群聊 + Web对话）...");
        
        int msgCount = keywordService.batchExtractFromMessages("all", 7);
        int convCount = keywordService.batchExtractFromConversations(7);
        int totalCount = msgCount + convCount;
        
        log.info("关键词同步完成: 群聊 {} 条, Web对话 {} 条, 总计 {} 条", msgCount, convCount, totalCount);
    }

    @Override
    @Scheduled(cron = "0 0 6 * * ?")
    public void cleanOldKeywords() {
        log.info("开始清理过期关键词...");
        
        keywordService.cleanOldKeywords(retentionDays);
        
        log.info("关键词清理完成");
    }
}