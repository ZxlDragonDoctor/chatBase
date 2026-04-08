package com.zxl.chatbase.im.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 群聊和知识库文档映射实体（一对一关系）
 */
@Data
@TableName("group_kb_mapping")
public class GroupKbMapping {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 群ID
     */
    private String groupId;

    /**
     * 知识库文档ID
     */
    private String kbDocumentId;

    /**
     * 记录创建时间
     */
    private LocalDateTime createTime;

    /**
     * 记录更新时间
     */
    private LocalDateTime updateTime;

    public  GroupKbMapping(String groupId,String kbDocumentId){
        this.groupId = groupId;
        this.kbDocumentId = kbDocumentId;
    }
}
