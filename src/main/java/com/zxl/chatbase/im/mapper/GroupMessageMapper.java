package com.zxl.chatbase.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxl.chatbase.im.dto.GroupSummaryVO;
import com.zxl.chatbase.im.entity.GroupMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupMessageMapper extends BaseMapper<GroupMessage> {

    @Select("SELECT ig.id, gm.platform, gm.group_id, ig.group_name, "
            + "COUNT(1) AS messageCount, MAX(gm.message_time) AS lastMessageTime, "
            + "ig.app_id AS appId, ig.app_name AS appName, ig.created_by AS createdBy "
            + "FROM group_message gm "
            + "LEFT JOIN im_group ig ON gm.platform = ig.platform AND gm.group_id = ig.group_id "
            + "WHERE gm.group_id IS NOT NULL AND CHAR_LENGTH(TRIM(gm.group_id)) > 0 "
            + "AND (gm.conversation_type IS NULL OR gm.conversation_type = 'group') "
            + "GROUP BY ig.id, gm.platform, gm.group_id, ig.group_name, ig.app_id, ig.app_name, ig.created_by "
            + "ORDER BY lastMessageTime DESC LIMIT 500")
    List<GroupSummaryVO> selectGroupSummaries();

    @Select("<script>"
            + "SELECT ig.id, gm.platform, gm.group_id, ig.group_name, "
            + "COUNT(1) AS messageCount, MAX(gm.message_time) AS lastMessageTime, "
            + "ig.app_id AS appId, ig.app_name AS appName, ig.created_by AS createdBy "
            + "FROM group_message gm "
            + "LEFT JOIN im_group ig ON gm.platform = ig.platform AND gm.group_id = ig.group_id "
            + "WHERE gm.group_id IN "
            + "<foreach item='gid' collection='groupIds' open='(' separator=',' close=')'>#{gid}</foreach> "
            + "AND (gm.conversation_type IS NULL OR gm.conversation_type = 'group') "
            + "GROUP BY ig.id, gm.platform, gm.group_id, ig.group_name, ig.app_id, ig.app_name, ig.created_by "
            + "ORDER BY lastMessageTime DESC LIMIT 500"
            + "</script>")
    List<GroupSummaryVO> selectGroupSummariesByGroups(@Param("groupIds") List<String> groupIds);

    @Select("SELECT COUNT(1) FROM group_message WHERE conversation_type = 'single' "
            + "AND platform = #{platform} AND conversation_id = #{conversationId}")
    long countByConversation(@Param("platform") String platform, @Param("conversationId") String conversationId);

    @Select("SELECT COUNT(1) FROM group_message WHERE conversation_type = 'single'")
    long countAllPrivateMessages();

    @Select("SELECT DISTINCT conversation_id FROM group_message WHERE conversation_type = 'single'")
    List<String> selectDistinctPrivateConversations();
}

