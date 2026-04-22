package com.zxl.chatbase.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxl.chatbase.chat.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatSessionMapper extends BaseMapper<ChatSession> {
}