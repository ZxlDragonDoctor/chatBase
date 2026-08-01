package com.zxl.chatbase.im.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zxl.chatbase.im.dto.ConversationSummaryVO;
import com.zxl.chatbase.im.entity.ImConversation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ImConversationMapper extends BaseMapper<ImConversation> {

    @Select("<script>"
            + "SELECT id, conversation_id, platform, user_id, user_nickname, title, "
            + "last_message, last_message_time, message_count, created_by, app_id, app_name "
            + "FROM im_conversation "
            + "WHERE status = 1 "
            + "AND conversation_type = 'single' "
            + "AND (created_by = #{userId} OR created_by IS NULL) "
            + "ORDER BY last_message_time DESC "
            + "LIMIT 500"
            + "</script>")
    List<ConversationSummaryVO> selectAccessibleConversations(@Param("userId") String userId);
}
