package com.zxl.chatbase.im.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BotManageMapper {

    @Select("<script>"
            + "SELECT COUNT(DISTINCT group_id) FROM group_message "
            + "WHERE platform = #{platform} "
            + "AND group_id IN "
            + "<foreach item='gid' collection='groupIds' open='(' separator=',' close=')'>#{gid}</foreach>"
            + "</script>")
    int countGroups(@Param("platform") String platform, @Param("groupIds") List<String> groupIds);

    @Select("<script>"
            + "SELECT COUNT(*) FROM group_message "
            + "WHERE platform = #{platform} AND DATE(message_time) = CURDATE() "
            + "AND group_id IN "
            + "<foreach item='gid' collection='groupIds' open='(' separator=',' close=')'>#{gid}</foreach>"
            + "</script>")
    int countTodayMessages(@Param("platform") String platform, @Param("groupIds") List<String> groupIds);

    @Select("<script>"
            + "SELECT COUNT(*) FROM group_message "
            + "WHERE platform = #{platform} "
            + "AND group_id IN "
            + "<foreach item='gid' collection='groupIds' open='(' separator=',' close=')'>#{gid}</foreach>"
            + "</script>")
    int countTotalMessages(@Param("platform") String platform, @Param("groupIds") List<String> groupIds);

    @Select("<script>"
            + "SELECT MAX(message_time) FROM group_message "
            + "WHERE platform = #{platform} "
            + "AND group_id IN "
            + "<foreach item='gid' collection='groupIds' open='(' separator=',' close=')'>#{gid}</foreach>"
            + "</script>")
    LocalDateTime getLastActiveTime(@Param("platform") String platform, @Param("groupIds") List<String> groupIds);
}
