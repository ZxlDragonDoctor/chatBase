package com.zxl.chatbase.statistics.dto;

import lombok.Data;

import java.util.List;

@Data
public class GroupActiveVO {
    private List<GroupRankItem> topGroups;
    private Long totalGroups;
    private Long totalMessages;

    @Data
    public static class GroupRankItem {
        private String platform;
        private String groupId;
        private String groupName;
        private Long messageCount;
        private String lastMessageTime;
        private Integer rank;
    }
}