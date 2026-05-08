package com.zxl.chatbase.statistics.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
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