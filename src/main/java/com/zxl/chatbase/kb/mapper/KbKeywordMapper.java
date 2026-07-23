package com.zxl.chatbase.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxl.chatbase.kb.entity.KbKeyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface KbKeywordMapper extends BaseMapper<KbKeyword> {

    @Update("INSERT INTO kb_keyword(keyword, source, count, last_seen_time, create_time, update_time) " +
            "VALUES(#{keyword}, #{source}, 1, NOW(), NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE count = count + 1, last_seen_time = NOW(), update_time = NOW()")
    int incrementCount(@Param("keyword") String keyword, @Param("source") String source);
}