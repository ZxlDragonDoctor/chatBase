package com.zxl.chatbase.kb.service;

import com.zxl.chatbase.statistics.dto.KeywordHotVO;

import java.util.List;

public interface IKbKeywordService {

    void extractAndSaveKeywords(String text, String source);

    KeywordHotVO getKeywordCloud(String source, Integer days, Integer limit);

    List<String> getTopKeywords(String source, Integer limit);

    void cleanOldKeywords(int retentionDays);

    int batchExtractFromMessages(String platform, int days);

    int batchExtractFromConversations(int days);

    int syncLatestKeywords(int limit);
}