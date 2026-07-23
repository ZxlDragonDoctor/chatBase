package com.zxl.chatbase.statistics.service;

import java.time.LocalDate;

public interface IStatisticsAggregateService {

    void aggregateDailyStatistics(LocalDate date);

    void aggregateYesterdayStatistics();

    void aggregateLast7Days();

    void fillMissingDates(int days);
}