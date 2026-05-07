package com.zxl.chatbase.im.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface BotManageMapper {

    @Select("SELECT COUNT(DISTINCT group_id) FROM group_message WHERE platform = #{platform}")
    int countGroups(String platform);

    @Select("SELECT COUNT(*) FROM group_message WHERE platform = #{platform} AND DATE(message_time) = CURDATE()")
    int countTodayMessages(String platform);

    @Select("SELECT COUNT(*) FROM group_message WHERE platform = #{platform}")
    int countTotalMessages(String platform);

    @Select("SELECT MAX(message_time) FROM group_message WHERE platform = #{platform}")
    LocalDateTime getLastActiveTime(String platform);
}
