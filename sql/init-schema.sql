-- MySQL 初始化建表脚本（给 docker-entrypoint-initdb.d 使用）
-- 目标库：chat_base

-- group_message：群聊消息表
CREATE TABLE IF NOT EXISTS group_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  platform VARCHAR(32) NOT NULL COMMENT '平台: qq / wecom',
  group_id VARCHAR(64) NOT NULL COMMENT '群ID（平台原始ID）',
  user_id VARCHAR(64) NOT NULL COMMENT '用户ID（平台原始ID）',
  message_id VARCHAR(64) NOT NULL COMMENT '平台消息ID',
  message_type VARCHAR(32) NOT NULL COMMENT '消息类型: text/image/file 等',
  raw_message TEXT NOT NULL COMMENT '原始消息内容（含CQ码）',
  message_time DATETIME DEFAULT NULL COMMENT '消息发送时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  synced TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已同步到知识库',
  kb_document_id VARCHAR(128) DEFAULT NULL COMMENT '知识库文档ID',
  PRIMARY KEY (id),
  KEY idx_group_time (group_id, message_time),
  KEY idx_synced (synced)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- group_kb_mapping：群聊和知识库文档映射表
CREATE TABLE IF NOT EXISTS group_kb_mapping (
  id BIGINT NOT NULL AUTO_INCREMENT,
  group_id VARCHAR(64) NOT NULL COMMENT '群ID',
  kb_document_id VARCHAR(128) NOT NULL COMMENT '知识库文档ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录更新时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS t_duty_chat_group(
 `rec_id` INT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
 `chat_group_url_id` VARCHAR(255) DEFAULT NULL COMMENT '值班群chatId',
 `chat_group_name` VARCHAR(20) NOT NULL COMMENT '值班群群名',
 `distribution_robot_url` VARCHAR(255) NOT NULL COMMENT '值班群分发机器人链接',
 `is_disabled` TINYINT(1) DEFAULT 0 COMMENT '是否禁用',
 `version` INT DEFAULT 0 COMMENT '版本号（乐观锁）',
 `cycle_span` INT DEFAULT 0 COMMENT '总共值班天数',
 `is_create_tapd` TINYINT(1) DEFAULT 0 COMMENT '是否创建TAPD',
 `sentinel_start_time` DATETIME DEFAULT NULL COMMENT '哨兵开始时间',
 `created` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 `modified` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
 `creator_id` INT DEFAULT NULL COMMENT '创建人ID',
 `duty_send_ids` VARCHAR(1024) NOT NULL COMMENT '值班群消息分发人ID列表（逗号分隔）',
 `duty_receive_ids` VARCHAR(1024) NOT NULL COMMENT '值班群消息接收人ID列表（逗号分隔）',
 `intell_robot_ids` VARCHAR(1024) NOT NULL COMMENT '智能机器人ID列表（逗号分隔）',
 PRIMARY KEY (`rec_id`),
 KEY `idx_chat_group_url_id` (`chat_group_url_id`),
 KEY `idx_creator_id` (`creator_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
