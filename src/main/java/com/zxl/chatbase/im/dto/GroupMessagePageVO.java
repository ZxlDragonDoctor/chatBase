package com.zxl.chatbase.im.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMessagePageVO {
    private List<GroupMessageItemVO> records;
    private long total;
    private int page;
    private int size;
}
