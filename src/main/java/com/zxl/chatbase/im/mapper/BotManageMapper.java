package com.zxl.chatbase.im.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface BotManageMapper {

    @Select("SELECT COUNT(DISTINCT group_id) FROM group_message "
            + "WHERE platform = #{platform} AND group_id IS NOT NULL AND group_id <> 'single'")
    int countGroups(@Param("platform") String platform);

    @Select("SELECT COUNT(*) FROM group_message "
            + "WHERE platform = #{platform} AND DATE(message_time) = CURDATE()")
    int countTodayMessages(@Param("platform") String platform);

    @Select("SELECT COUNT(*) FROM group_message "
            + "WHERE platform = #{platform}")
    int countTotalMessages(@Param("platform") String platform);

    @Select("SELECT MAX(message_time) FROM group_message "
            + "WHERE platform = #{platform}")
    LocalDateTime getLastActiveTime(@Param("platform") String platform);
}