package com.zxl.chatbase.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeywordHotVO {
    private List<KeywordItem> keywords;
    private String platform;
    private String groupId;

    @Data
    public static class KeywordItem {
        private String keyword;
        private Integer count;
        private Integer rank;
    }
}