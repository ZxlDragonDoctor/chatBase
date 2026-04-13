package com.zxl.chatbase.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxl.chatbase.im.dto.GroupSummaryVO;
import com.zxl.chatbase.im.entity.GroupMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GroupMessageMapper extends BaseMapper<GroupMessage> {

    @Select("SELECT COALESCE(platform,'unknown') AS platform, group_id, "
            + "COUNT(1) AS messageCount, MAX(message_time) AS lastMessageTime "
            + "FROM group_message WHERE group_id IS NOT NULL AND CHAR_LENGTH(TRIM(group_id)) > 0 "
            + "GROUP BY COALESCE(platform,'unknown'), group_id "
            + "ORDER BY lastMessageTime DESC LIMIT 500")
    List<GroupSummaryVO> selectGroupSummaries();
}

