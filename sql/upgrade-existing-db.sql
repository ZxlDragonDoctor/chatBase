-- ============================================================
-- chatBase 已有数据库升级脚本（幂等，可重复执行）
-- 适用：服务器 chat_base 库已有旧数据（volume 未重新初始化）
-- 目标：补齐新代码所需的表 / 字段 / 索引
-- ============================================================
USE chat_base;

-- ---------- 1. 补齐缺失的表 ----------

-- im_conversation ：IM 单聊会话表（私聊采集 / opencode 绑定依赖，可能缺失）
CREATE TABLE IF NOT EXISTS `im_conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `platform` VARCHAR(32) NOT NULL COMMENT '平台：qq/wecom/wx',
  `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话唯一标识',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID（平台原始ID）',
  `user_nickname` VARCHAR(100) DEFAULT NULL COMMENT '用户昵称',
  `conversation_type` VARCHAR(16) NOT NULL DEFAULT 'single' COMMENT '会话类型：single-单聊',
  `last_message` TEXT COMMENT '最后一条消息内容',
  `last_message_time` DATETIME DEFAULT NULL COMMENT '最后消息时间',
  `message_count` INT NOT NULL DEFAULT 0 COMMENT '消息数量',
  `created_by` VARCHAR(50) DEFAULT NULL COMMENT '归属用户名（用于数据隔离）',
  `app_id` BIGINT DEFAULT NULL COMMENT '绑定应用ID（-1 表示本地opencode）',
  `app_name` VARCHAR(100) DEFAULT NULL COMMENT '应用名称（冗余字段）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_conv` (`platform`, `conversation_id`),
  KEY `idx_platform_user` (`platform`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_last_message_time` (`last_message_time`),
  KEY `idx_app_id` (`app_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IM单聊会话表';

-- kb_keyword ：关键词统计表（可能缺失）
CREATE TABLE IF NOT EXISTS `kb_keyword` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `keyword` VARCHAR(100) NOT NULL COMMENT '关键词',
  `source` VARCHAR(20) NOT NULL DEFAULT 'conversation' COMMENT '来源：conversation-对话/query, im-IM消息, document-文档',
  `count` INT NOT NULL DEFAULT 1 COMMENT '出现次数',
  `last_seen_time` DATETIME DEFAULT NULL COMMENT '最后出现时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_keyword_source` (`keyword`, `source`),
  KEY `idx_count` (`count` DESC),
  KEY `idx_source` (`source`),
  KEY `idx_last_seen` (`last_seen_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='关键词统计表';

-- ---------- 2. 按需添加列（幂等） ----------

-- im_group：应用绑定 + 归属人
SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='chat_base' AND TABLE_NAME='im_group' AND COLUMN_NAME='app_id');
SET @sql = IF(@has=0, 'ALTER TABLE `im_group` ADD COLUMN `app_id` BIGINT DEFAULT NULL COMMENT ''绑定的应用ID'' AFTER `kb_id`;', 'SELECT ''im_group.app_id already exists'';');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='chat_base' AND TABLE_NAME='im_group' AND COLUMN_NAME='app_name');
SET @sql = IF(@has=0, 'ALTER TABLE `im_group` ADD COLUMN `app_name` VARCHAR(100) DEFAULT NULL COMMENT ''应用名称（冗余字段）'' AFTER `app_id`;', 'SELECT ''im_group.app_name already exists'';');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='chat_base' AND TABLE_NAME='im_group' AND COLUMN_NAME='created_by');
SET @sql = IF(@has=0, 'ALTER TABLE `im_group` ADD COLUMN `created_by` VARCHAR(50) DEFAULT NULL COMMENT ''归属用户（创建者用户名）'' AFTER `kb_id`;', 'SELECT ''im_group.created_by already exists'';');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='chat_base' AND TABLE_NAME='im_group' AND INDEX_NAME='idx_created_by');
SET @sql = IF(@has=0, 'ALTER TABLE `im_group` ADD KEY `idx_created_by` (`created_by`);', 'SELECT ''im_group.idx_created_by already exists'';');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- im_conversation：应用绑定字段（表可能新建后缺失）
SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='chat_base' AND TABLE_NAME='im_conversation' AND COLUMN_NAME='app_id');
SET @sql = IF(@has=0, 'ALTER TABLE `im_conversation` ADD COLUMN `app_id` BIGINT DEFAULT NULL COMMENT ''绑定应用ID'' AFTER `created_by`;', 'SELECT ''im_conversation.app_id already exists'';');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='chat_base' AND TABLE_NAME='im_conversation' AND COLUMN_NAME='app_name');
SET @sql = IF(@has=0, 'ALTER TABLE `im_conversation` ADD COLUMN `app_name` VARCHAR(100) DEFAULT NULL COMMENT ''应用名称（冗余字段）'' AFTER `app_id`;', 'SELECT ''im_conversation.app_name already exists'';');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- kb_conversation：应用关联 + session_id + 费用字段
SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='chat_base' AND TABLE_NAME='kb_conversation' AND COLUMN_NAME='app_id');
SET @sql = IF(@has=0, 'ALTER TABLE `kb_conversation` ADD COLUMN `app_id` BIGINT DEFAULT NULL COMMENT ''使用的应用ID'' AFTER `knowledge_base_id`;', 'SELECT ''kb_conversation.app_id already exists'';');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='chat_base' AND TABLE_NAME='kb_conversation' AND COLUMN_NAME='app_name');
SET @sql = IF(@has=0, 'ALTER TABLE `kb_conversation` ADD COLUMN `app_name` VARCHAR(100) DEFAULT NULL COMMENT ''应用名称'' AFTER `app_id`;', 'SELECT ''kb_conversation.app_name already exists'';');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='chat_base' AND TABLE_NAME='kb_conversation' AND COLUMN_NAME='session_id');
SET @sql = IF(@has=0, 'ALTER TABLE `kb_conversation` ADD COLUMN `session_id` VARCHAR(64) DEFAULT NULL COMMENT ''关联会话ID'' AFTER `id`;', 'SELECT ''kb_conversation.session_id already exists'';');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='chat_base' AND TABLE_NAME='kb_conversation' AND COLUMN_NAME='prompt_tokens');
SET @sql = IF(@has=0, 'ALTER TABLE `kb_conversation` ADD COLUMN `prompt_tokens` INT DEFAULT 0 COMMENT ''提示词tokens'' AFTER `tokens`, ADD COLUMN `completion_tokens` INT DEFAULT 0 COMMENT ''完成tokens'' AFTER `prompt_tokens`, ADD COLUMN `prompt_price` DECIMAL(10,6) DEFAULT 0 COMMENT ''提示词费用'' AFTER `completion_tokens`, ADD COLUMN `completion_price` DECIMAL(10,6) DEFAULT 0 COMMENT ''完成费用'' AFTER `prompt_price`, ADD COLUMN `total_price` DECIMAL(10,6) DEFAULT 0 COMMENT ''总费用'' AFTER `completion_price`;', 'SELECT ''kb_conversation cost fields already exist'';');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- kb_statistics：应用维度 + 费用统计（注意：列在 init 中已有，部分为增量）
SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='chat_base' AND TABLE_NAME='kb_statistics' AND COLUMN_NAME='app_id');
SET @sql = IF(@has=0, 'ALTER TABLE `kb_statistics` ADD COLUMN `app_id` BIGINT DEFAULT NULL COMMENT ''应用ID'' AFTER `knowledge_base_id`;', 'SELECT ''kb_statistics.app_id already exists'';');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='chat_base' AND TABLE_NAME='kb_statistics' AND COLUMN_NAME='total_cost');
SET @sql = IF(@has=0, 'ALTER TABLE `kb_statistics` ADD COLUMN `total_prompt_tokens` BIGINT DEFAULT 0 COMMENT ''总提示词tokens'' AFTER `total_tokens`, ADD COLUMN `total_completion_tokens` BIGINT DEFAULT 0 COMMENT ''总完成tokens'' AFTER `total_prompt_tokens`, ADD COLUMN `total_cost` DECIMAL(12,4) DEFAULT 0 COMMENT ''总费用'' AFTER `avg_latency_ms`, ADD COLUMN `avg_cost` DECIMAL(10,6) DEFAULT 0 COMMENT ''平均费用'' AFTER `total_cost`;', 'SELECT ''kb_statistics cost fields already exist'';');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- kb_knowledge_base：is_public 权限字段
SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='chat_base' AND TABLE_NAME='kb_knowledge_base' AND COLUMN_NAME='is_public');
SET @sql = IF(@has=0, 'ALTER TABLE `kb_knowledge_base` ADD COLUMN `is_public` TINYINT(1) NOT NULL DEFAULT 1 COMMENT ''是否公开：0-仅创建者，1-所有用户可用'' AFTER `status`;', 'SELECT ''kb_knowledge_base.is_public already exists'';');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- kb_feedback：conversation_id 允许为空
SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='chat_base' AND TABLE_NAME='kb_feedback' AND COLUMN_NAME='conversation_id' AND IS_NULLABLE='YES');
SET @sql = IF(@has=0, 'ALTER TABLE `kb_feedback` MODIFY COLUMN `conversation_id` BIGINT DEFAULT NULL COMMENT ''关联会话ID（可选）'';', 'SELECT ''kb_feedback.conversation_id already nullable'';');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- group_message 会话字段（单聊）
SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='chat_base' AND TABLE_NAME='group_message' AND COLUMN_NAME='conversation_type');
SET @sql = IF(@has=0, 'ALTER TABLE `group_message` ADD COLUMN `conversation_type` VARCHAR(16) NOT NULL DEFAULT ''group'' COMMENT ''会话类型：group-群聊，single-单聊'' AFTER `platform`;', 'SELECT ''group_message.conversation_type already exists'';');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

SET @has = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='chat_base' AND TABLE_NAME='group_message' AND COLUMN_NAME='conversation_id');
SET @sql = IF(@has=0, 'ALTER TABLE `group_message` ADD COLUMN `conversation_id` VARCHAR(64) DEFAULT NULL COMMENT ''会话ID（单聊时使用）'' AFTER `group_id`;', 'SELECT ''group_message.conversation_id already exists'';');
PREPARE s FROM @sql; EXECUTE s; DEALLOCATE PREPARE s;

-- ---------- 3. 默认 sys_config ----------
INSERT IGNORE INTO `sys_config` (`config_key`, `config_value`, `config_type`, `config_group`, `config_desc`, `sort_order`) VALUES
('dify.chat.timeout', '90', 'number', 'dify', 'Dify对话超时时间（秒）', 10),
('dify.chat.maxTurns', '20', 'number', 'dify', '单会话最大轮数', 20),
('qq.rateLimit.enabled', 'true', 'boolean', 'qq', '是否启用QQ限流', 30),
('qq.rateLimit.perUser', '10', 'number', 'qq', '每用户每分钟最大请求数', 31),
('qq.rateLimit.perGroup', '30', 'number', 'qq', '每群每分钟最大请求数', 32),
('web.rateLimit.enabled', 'true', 'boolean', 'web', '是否启用Web限流', 40),
('web.rateLimit.perUser', '30', 'number', 'web', '每用户每分钟最大请求数', 41),
('sync.batchSize', '200', 'number', 'sync', '知识库同步批次大小', 50),
('sync.interval', '60', 'number', 'sync', '知识库同步间隔（秒）', 51),
('redis.conversation.ttl', '7', 'number', 'redis', 'Redis会话过期天数', 60);