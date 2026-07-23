-- 会话管理表
CREATE TABLE IF NOT EXISTS `chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `session_id` VARCHAR(64) NOT NULL COMMENT '会话唯一标识',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `channel` VARCHAR(20) NOT NULL DEFAULT 'web' COMMENT '渠道：web/im/wx',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '会话标题（首条消息摘要）',
  `dify_conversation_id` VARCHAR(64) DEFAULT NULL COMMENT 'Dify会话ID',
  `message_count` INT NOT NULL DEFAULT 0 COMMENT '消息数量',
  `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-已删除，1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_id` (`session_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_channel` (`channel`),
  KEY `idx_last_message_time` (`last_message_time`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天会话表';

-- 修改 kb_conversation 表，添加 session_id 关联
ALTER TABLE `kb_conversation` ADD COLUMN `session_id` VARCHAR(64) DEFAULT NULL COMMENT '关联会话ID' AFTER `id`;
ALTER TABLE `kb_conversation` ADD KEY `idx_session_id` (`session_id`);