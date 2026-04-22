-- 修改 kb_feedback 表，允许 conversation_id 为空
ALTER TABLE `kb_feedback` MODIFY COLUMN `conversation_id` BIGINT DEFAULT NULL COMMENT '关联会话ID（可选）';