package com.zxl.chatbase.kb.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_user_category_mapping")
public class KbUserCategoryMapping {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String userId;
    
    private Long kbId;
    
    private Long categoryId;
    
    private LocalDateTime createTime;
}
