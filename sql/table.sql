create database IF NOT EXISTS chat_base;

CREATE TABLE `group_message` (
                                 `id` bigint NOT NULL AUTO_INCREMENT,
                                 `platform` varchar(32) NOT NULL COMMENT '平台: qq / wecom',
                                 `group_id` varchar(64) NOT NULL COMMENT '群ID（平台原始ID）',
                                 `user_id` varchar(64) NOT NULL COMMENT '用户ID（平台原始ID）',
                                 `message_id` varchar(64) NOT NULL COMMENT '平台消息ID',
                                 `message_type` varchar(32) NOT NULL COMMENT '消息类型: text/image/file 等',
                                 `raw_message` text NOT NULL COMMENT '原始消息内容（含CQ码）',
                                 `message_time` datetime DEFAULT NULL COMMENT '消息发送时间',
                                 `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
                                 PRIMARY KEY (`id`),
                                 KEY `idx_group_time` (`group_id`,`message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='群聊消息表';


ALTER TABLE `group_message`
    ADD COLUMN `synced` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已同步到知识库',
    ADD COLUMN `kb_document_id` varchar(128) DEFAULT NULL COMMENT '知识库文档ID';

ALTER TABLE `group_message`
    ADD KEY `idx_synced` (`synced`);

show create table group_message;

select * from group_message;