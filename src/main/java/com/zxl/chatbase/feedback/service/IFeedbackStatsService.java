package com.zxl.chatbase.feedback.service;

import java.util.Map;
import java.util.Set;

public interface IFeedbackStatsService {

    boolean recordThumbsUp(String sessionId, int messageIndex);

    boolean recordThumbsDown(String sessionId, int messageIndex);

    Map<String, Object> getDailyStats(int days);

    Map<String, Object> getOverallStats();

    long getThumbsUpCount(int days);

    long getThumbsDownCount(int days);

    double getPositiveRate();

    Map<Integer, Integer> getSessionFeedbackStatus(String sessionId);
}