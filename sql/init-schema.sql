-- MySQL 初始化建表脚本（给 docker-entrypoint-initdb.d 使用）
-- 目标库：chat_base

-- =============================================
-- 基础模块：系统用户
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(128) NOT NULL COMMENT '密码（加密存储）',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `role` VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色：admin-管理员，user-普通用户',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `last_login_ip` VARCHAR(50) DEFAULT NULL COMMENT '最后登录IP',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除：0-否，1-是',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- =============================================
-- 知识库模块：分类管理
-- =============================================
CREATE TABLE IF NOT EXISTS `kb_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID，0为顶级',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `icon` VARCHAR(255) DEFAULT NULL COMMENT '分类图标',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序，数字越小越靠前',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '分类描述',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名',
  PRIMARY KEY (`id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库分类表';

-- =============================================
-- 知识库模块：知识库管理
-- =============================================
CREATE TABLE IF NOT EXISTS `kb_knowledge_base` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '知识库ID',
  `name` VARCHAR(100) NOT NULL COMMENT '知识库名称',
  `description` TEXT COMMENT '知识库描述',
  `category_id` BIGINT DEFAULT NULL COMMENT '所属分类ID',
  `dify_dataset_id` VARCHAR(128) DEFAULT NULL COMMENT 'Dify知识库ID',
  `dify_api_key` VARCHAR(255) DEFAULT NULL COMMENT 'Dify API Key',
  `source_type` VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '来源类型：manual-手动创建，im_sync-IM同步',
  `sync_platform` VARCHAR(32) DEFAULT NULL COMMENT '同步平台：qq/wecom',
  `sync_group_ids` TEXT COMMENT '同步群ID列表（JSON数组）',
  `auto_sync` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否自动同步：0-否，1-是',
  `sync_interval` INT NOT NULL DEFAULT 60 COMMENT '同步间隔（分钟）',
  `doc_count` INT NOT NULL DEFAULT 0 COMMENT '文档数量',
  `chunk_count` INT NOT NULL DEFAULT 0 COMMENT '切片数量',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `is_public` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否公开：0-仅创建者，1-所有用户可用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_dify_dataset_id` (`dify_dataset_id`),
  KEY `idx_source_type` (`source_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库管理表';

-- =============================================
-- 知识库模块：文档管理
-- =============================================
CREATE TABLE IF NOT EXISTS `kb_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
  `knowledge_base_id` BIGINT NOT NULL COMMENT '所属知识库ID',
  `title` VARCHAR(200) NOT NULL COMMENT '文档标题',
  `content` LONGTEXT COMMENT '文档内容',
  `file_url` VARCHAR(500) DEFAULT NULL COMMENT '原始文件URL',
  `file_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
  `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型',
  `dify_document_id` VARCHAR(128) DEFAULT NULL COMMENT 'Dify文档ID',
  `dify_status` VARCHAR(32) DEFAULT NULL COMMENT 'Dify处理状态：pending/processing/completed/failed',
  `dify_chunk_count` INT DEFAULT 0 COMMENT 'Dify切片数量',
  `source` VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '来源：manual-手动上传，im_sync-IM同步',
  `source_message_id` VARCHAR(64) DEFAULT NULL COMMENT '来源消息ID（IM同步时）',
  `sync_status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '同步状态：0-未同步，1-已同步，2-同步失败',
  `sync_time` DATETIME DEFAULT NULL COMMENT '同步时间',
  `sync_error` TEXT COMMENT '同步错误信息',
  `tag_list` VARCHAR(500) DEFAULT NULL COMMENT '标签列表（逗号分隔）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_base_id` (`knowledge_base_id`),
  KEY `idx_dify_document_id` (`dify_document_id`),
  KEY `idx_sync_status` (`sync_status`),
  KEY `idx_source` (`source`),
  FULLTEXT KEY `ft_title_content` (`title`, `content`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档表';

-- =============================================
-- 知识库模块：会话记录
-- =============================================
CREATE TABLE IF NOT EXISTS `kb_conversation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `session_id` VARCHAR(64) DEFAULT NULL COMMENT '关联会话ID',
  `conversation_id` VARCHAR(64) NOT NULL COMMENT '会话唯一标识（Dify返回）',
  `user_id` VARCHAR(64) NOT NULL COMMENT '用户ID',
  `user_nickname` VARCHAR(50) DEFAULT NULL COMMENT '用户昵称',
  `channel` VARCHAR(20) NOT NULL COMMENT '渠道：web/im/wx',
  `group_id` VARCHAR(64) DEFAULT NULL COMMENT '群ID（IM渠道）',
  `knowledge_base_id` BIGINT DEFAULT NULL COMMENT '关联知识库ID',
  `query` TEXT NOT NULL COMMENT '用户问题',
  `answer` LONGTEXT COMMENT 'AI回答',
  `dify_response_id` VARCHAR(64) DEFAULT NULL COMMENT 'Dify响应ID',
  `tokens` INT DEFAULT 0 COMMENT '消耗tokens',
  `prompt_tokens` INT DEFAULT 0 COMMENT '提示词tokens',
  `completion_tokens` INT DEFAULT 0 COMMENT '完成tokens',
  `prompt_price` DECIMAL(10,6) DEFAULT 0 COMMENT '提示词费用',
  `completion_price` DECIMAL(10,6) DEFAULT 0 COMMENT '完成费用',
  `total_price` DECIMAL(10,6) DEFAULT 0 COMMENT '总费用',
  `latency_ms` INT DEFAULT 0 COMMENT '响应延迟（毫秒）',
  `source_documents` TEXT COMMENT '引用的知识库文档（JSON）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-失败，1-成功',
  `error_message` TEXT COMMENT '错误信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_conversation_id` (`conversation_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_channel` (`channel`),
  KEY `idx_knowledge_base_id` (`knowledge_base_id`),
  KEY `idx_create_time` (`create_time`),
  KEY `idx_session_id` (`session_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话记录表';

-- =============================================
-- 知识库模块：用户反馈
-- =============================================
CREATE TABLE IF NOT EXISTS `kb_feedback` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '反馈ID',
  `conversation_id` BIGINT NOT NULL COMMENT '关联会话ID',
  `user_id` VARCHAR(64) NOT NULL COMMENT '反馈用户ID',
  `rating` TINYINT NOT NULL COMMENT '评分：1-差，2-一般，3-好，4-很好，5-极好',
  `feedback_type` VARCHAR(32) DEFAULT NULL COMMENT '反馈类型：accurate/inaccurate/partial/off_topic',
  `feedback_content` TEXT COMMENT '反馈内容',
  `admin_reply` TEXT COMMENT '管理员回复',
  `admin_id` BIGINT DEFAULT NULL COMMENT '处理管理员ID',
  `reply_time` DATETIME DEFAULT NULL COMMENT '回复时间',
  `status` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '处理状态：0-待处理，1-已回复',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '反馈时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_conversation_id` (`conversation_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_rating` (`rating`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户反馈表';

-- =============================================
-- 知识库模块：常见问答（FAQ）
-- =============================================
CREATE TABLE IF NOT EXISTS `kb_faq` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'FAQ ID',
  `knowledge_base_id` BIGINT NOT NULL COMMENT '所属知识库ID',
  `category_id` BIGINT DEFAULT NULL COMMENT '所属分类ID',
  `question` VARCHAR(500) NOT NULL COMMENT '问题',
  `answer` LONGTEXT NOT NULL COMMENT '答案',
  `keywords` VARCHAR(500) DEFAULT NULL COMMENT '关键词（逗号分隔，用于匹配）',
  `hit_count` INT NOT NULL DEFAULT 0 COMMENT '命中次数',
  `satisfaction` DECIMAL(3,2) DEFAULT NULL COMMENT '满意度（0.00-1.00）',
  `similar_questions` TEXT COMMENT '相似问题（JSON数组）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级，数字越大优先级越高',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
  PRIMARY KEY (`id`),
  KEY `idx_knowledge_base_id` (`knowledge_base_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_hit_count` (`hit_count`),
  KEY `idx_priority` (`priority`),
  FULLTEXT KEY `ft_question` (`question`, `keywords`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='常见问答表';

-- =============================================
-- 知识库模块：每日统计
-- =============================================
CREATE TABLE IF NOT EXISTS `kb_statistics` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '统计ID',
  `stat_date` DATE NOT NULL COMMENT '统计日期',
  `channel` VARCHAR(20) DEFAULT 'all' COMMENT '渠道：web/im/wx/all',
  `knowledge_base_id` BIGINT DEFAULT NULL COMMENT '知识库ID',
  `conversation_count` INT NOT NULL DEFAULT 0 COMMENT '会话数',
  `message_count` INT NOT NULL DEFAULT 0 COMMENT '消息数',
  `user_count` INT NOT NULL DEFAULT 0 COMMENT '独立用户数',
  `avg_tokens` DECIMAL(10,2) DEFAULT 0 COMMENT '平均消耗tokens',
  `total_tokens` BIGINT NOT NULL DEFAULT 0 COMMENT '总消耗tokens',
  `total_prompt_tokens` BIGINT DEFAULT 0 COMMENT '总提示词tokens',
  `total_completion_tokens` BIGINT DEFAULT 0 COMMENT '总完成tokens',
  `avg_latency_ms` INT DEFAULT 0 COMMENT '平均响应延迟',
  `total_cost` DECIMAL(12,4) DEFAULT 0 COMMENT '总费用',
  `avg_cost` DECIMAL(10,6) DEFAULT 0 COMMENT '平均费用',
  `feedback_count` INT NOT NULL DEFAULT 0 COMMENT '反馈数',
  `positive_feedback` INT NOT NULL DEFAULT 0 COMMENT '正面反馈数',
  `negative_feedback` INT NOT NULL DEFAULT 0 COMMENT '负面反馈数',
  `doc_sync_count` INT NOT NULL DEFAULT 0 COMMENT '文档同步数',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_date_channel_kb` (`stat_date`, `channel`, `knowledge_base_id`),
  KEY `idx_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日统计表';

-- =============================================
-- IM消息模块（原group_message扩展）
-- =============================================
-- group_message：群聊消息表
CREATE TABLE IF NOT EXISTS group_message (
  id BIGINT NOT NULL AUTO_INCREMENT,
  platform VARCHAR(32) NOT NULL COMMENT '平台: qq / wecom / wx',
  conversation_type VARCHAR(16) NOT NULL DEFAULT 'group' COMMENT '会话类型：group-群聊，single-单聊',
  group_id VARCHAR(64) DEFAULT NULL COMMENT '群ID（平台原始ID，群聊时必填）',
  conversation_id VARCHAR(64) DEFAULT NULL COMMENT '会话ID（单聊时使用，格式：single:{platform}:{userId}）',
  user_id VARCHAR(64) NOT NULL COMMENT '用户ID（平台原始ID）',
  message_id VARCHAR(64) NOT NULL COMMENT '平台消息ID',
  message_type VARCHAR(32) NOT NULL COMMENT '消息类型: text/image/file 等',
  raw_message TEXT NOT NULL COMMENT '原始消息内容（含CQ码）',
  message_time DATETIME DEFAULT NULL COMMENT '消息发送时间',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
  synced TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否已同步到知识库',
  kb_document_id VARCHAR(128) DEFAULT NULL COMMENT '知识库文档ID',
  file_url VARCHAR(500) DEFAULT NULL COMMENT '文件URL（图片/文件链接）',
  dify_file_id VARCHAR(128) DEFAULT NULL COMMENT 'Dify文件ID',
  file_name VARCHAR(255) DEFAULT NULL COMMENT '文件名',
  PRIMARY KEY (id),
  KEY idx_group_time (group_id, message_time),
  KEY idx_synced (synced)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 修改message_id字段运行为空
ALTER TABLE group_message
    MODIFY COLUMN message_id VARCHAR(100) DEFAULT '' NOT NULL;

-- 添加唯一索引防止重复消息（数据库兜底）
ALTER TABLE group_message
    ADD UNIQUE INDEX uk_platform_msgid (platform, message_id);

-- 添加更新时间字段
ALTER TABLE group_message
    ADD COLUMN update_time DATETIME DEFAULT NULL COMMENT '更新时间' AFTER create_time;

-- 添加会话类型字段（group-群聊, single-单聊）
ALTER TABLE group_message
    ADD COLUMN conversation_type VARCHAR(16) NOT NULL DEFAULT 'group' COMMENT '会话类型：group-群聊，single-单聊' AFTER platform;

-- 添加会话ID字段（单聊时使用，格式：single:{platform}:{userId}）
ALTER TABLE group_message
    ADD COLUMN conversation_id VARCHAR(64) DEFAULT NULL COMMENT '会话ID（单聊时使用）' AFTER group_id;



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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日统计表';

-- =============================================
-- 文件模块：文件管理
-- =============================================
CREATE TABLE IF NOT EXISTS `kb_file` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
  `file_path` VARCHAR(500) NOT NULL COMMENT '文件存储路径',
  `file_size` BIGINT NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `file_type` VARCHAR(100) DEFAULT NULL COMMENT '文件类型（MIME）',
  `file_ext` VARCHAR(20) DEFAULT NULL COMMENT '文件扩展名',
  `bucket` VARCHAR(50) DEFAULT 'local' COMMENT '存储桶：local/oss/s3',
  `source` VARCHAR(32) NOT NULL DEFAULT 'manual' COMMENT '来源：manual/manual_upload/im_sync/web',
  `source_id` VARCHAR(64) DEFAULT NULL COMMENT '来源ID（IM同步时为group_message表ID）',
  `upload_user_id` VARCHAR(64) DEFAULT NULL COMMENT '上传用户ID',
  `upload_group_id` VARCHAR(64) DEFAULT NULL COMMENT '上传群ID（IM渠道）',
  `dify_file_id` VARCHAR(128) DEFAULT NULL COMMENT 'Dify文件ID',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_source` (`source`),
  KEY `idx_source_id` (`source_id`),
  KEY `idx_upload_user_id` (`upload_user_id`),
  KEY `idx_dify_file_id` (`dify_file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件管理表';

#  word或者奇特类型的文件类型格式比较长
ALTER TABLE kb_file MODIFY COLUMN file_type VARCHAR(100) DEFAULT NULL COMMENT
    '文件类型（MIME）';

-- =============================================
-- 系统模块：系统配置
-- =============================================
CREATE TABLE IF NOT EXISTS `sys_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` TEXT COMMENT '配置值',
  `config_type` VARCHAR(20) NOT NULL DEFAULT 'string' COMMENT '类型：string/number/boolean/json',
  `config_group` VARCHAR(50) NOT NULL DEFAULT 'default' COMMENT '配置分组',
  `config_desc` VARCHAR(255) DEFAULT NULL COMMENT '配置描述',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_config_group` (`config_group`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 默认系统配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `config_type`, `config_group`, `config_desc`, `sort_order`) VALUES
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

-- =============================================
-- IM模块：群组信息表
-- =============================================
CREATE TABLE IF NOT EXISTS `im_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '群组ID',
  `platform` VARCHAR(32) NOT NULL COMMENT '平台：qq/wecom',
  `group_id` VARCHAR(64) NOT NULL COMMENT '平台群ID',
  `group_name` VARCHAR(100) DEFAULT NULL COMMENT '群名称',
  `member_count` INT NOT NULL DEFAULT 0 COMMENT '成员数',
  `owner_id` VARCHAR(64) DEFAULT NULL COMMENT '群主ID',
  `robot_id` VARCHAR(64) DEFAULT NULL COMMENT '机器人ID',
  `auto_reply` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否自动回复：0-否，1-是',
  `kb_id` BIGINT DEFAULT NULL COMMENT '关联知识库ID',
  `created_by` VARCHAR(50) DEFAULT NULL COMMENT '归属用户（创建者用户名）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_group` (`platform`, `group_id`),
  KEY `idx_kb_id` (`kb_id`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IM群组信息表';

-- =============================================
-- IM模块：用户信息表
-- =============================================
CREATE TABLE IF NOT EXISTS `im_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `platform` VARCHAR(32) NOT NULL COMMENT '平台：qq/wecom',
  `user_id` VARCHAR(64) NOT NULL COMMENT '平台用户ID',
  `nickname` VARCHAR(100) DEFAULT NULL COMMENT '昵称',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像',
  `role` VARCHAR(20) NOT NULL DEFAULT 'member' COMMENT '角色：owner/admin/member',
  `group_id` VARCHAR(64) DEFAULT NULL COMMENT '所属群ID',
  `last_message_time` DATETIME DEFAULT NULL COMMENT '最后发言时间',
  `message_count` INT NOT NULL DEFAULT 0 COMMENT '消息数',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
PRIMARY KEY (`id`),
    UNIQUE KEY `uk_platform_user` (`platform`, `user_id`),
    KEY `idx_group_id` (`group_id`),
    KEY `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IM用户信息表';

-- =============================================
-- 聊天会话管理表
-- =============================================
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

-- =============================================
-- IM模块：单聊会话表（支持QQ/企微/微信单聊）
-- =============================================
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
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_conv` (`platform`, `conversation_id`),
  KEY `idx_platform_user` (`platform`, `user_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_created_by` (`created_by`),
  KEY `idx_last_message_time` (`last_message_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IM单聊会话表';

ALTER TABLE `im_conversation` ADD COLUMN `app_id` BIGINT DEFAULT NULL COMMENT '绑定应用ID' AFTER `created_by`;
ALTER TABLE `im_conversation` ADD COLUMN `app_name` VARCHAR(100) DEFAULT NULL COMMENT '应用名称（冗余字段）' AFTER `app_id`;
ALTER TABLE `im_conversation` ADD KEY `idx_app_id` (`app_id`);

-- =============================================
-- 关键词统计表
-- =============================================
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

-- =============================================
-- 应用模块：应用配置表
-- =============================================
CREATE TABLE IF NOT EXISTS `kb_app` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '应用ID',
  `name` VARCHAR(100) NOT NULL COMMENT '应用名称',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '应用描述',
  `icon` VARCHAR(50) DEFAULT NULL COMMENT '应用图标',
  `dify_api_key` VARCHAR(100) NOT NULL COMMENT 'Dify应用API Key',
  `dify_app_name` VARCHAR(100) DEFAULT NULL COMMENT 'Dify应用名称（从/v1/info获取）',
  `dify_app_mode` VARCHAR(50) DEFAULT NULL COMMENT 'Dify应用模式：chatbot/agent/workflow/completion',
  `category_id` BIGINT DEFAULT NULL COMMENT '关联的分类ID',
  `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为默认应用',
  `is_public` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否公开：0-仅创建者可用，1-所有用户可用',
  `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建者',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_create_by` (`create_by`),
  KEY `idx_status` (`status`),
  KEY `idx_is_default` (`is_default`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='应用配置表';


-- 初始化默认应用（使用配置文件中的默认API Key）
-- 注意：实际部署时需要替换为真实的API Key
INSERT INTO `kb_app` (`name`, `description`, `icon`, `dify_api_key`, `dify_app_name`, `dify_app_mode`, `is_default`, `is_public`, `create_by`, `status`, `create_time`, `update_time`)
VALUES ('默认助手', '系统默认应用，使用配置文件中的API Key', 'robot', 'PLACEHOLDER_API_KEY', NULL, NULL, 1, 1, 'admin', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `update_time` = NOW();

-- =============================================
-- 知识库模块：用户分类关联表（将公开知识库关联到用户分类）
-- =============================================
CREATE TABLE IF NOT EXISTS `kb_user_category_mapping` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `kb_id` BIGINT NOT NULL COMMENT '知识库ID',
  `user_id` VARCHAR(50) NOT NULL COMMENT '关联用户',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_kb_user` (`category_id`, `kb_id`, `user_id`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_kb_id` (`kb_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户分类关联表';

-- =============================================
-- 已有数据库升级脚本（ALTER语句）
-- 注意：新部署会自动创建完整表结构，以下仅用于已有数据库升级
-- =============================================

-- im_group表添加应用绑定字段
ALTER TABLE `im_group` ADD COLUMN `app_id` BIGINT DEFAULT NULL COMMENT '绑定的应用ID' AFTER `kb_id`;
ALTER TABLE `im_group` ADD COLUMN `app_name` VARCHAR(100) DEFAULT NULL COMMENT '应用名称（冗余字段）' AFTER `app_id`;
ALTER TABLE `im_group` ADD KEY `idx_app_id` (`app_id`);

-- kb_conversation表添加应用关联字段
ALTER TABLE `kb_conversation` ADD COLUMN `app_id` BIGINT DEFAULT NULL COMMENT '使用的应用ID' AFTER `knowledge_base_id`;
ALTER TABLE `kb_conversation` ADD COLUMN `app_name` VARCHAR(100) DEFAULT NULL COMMENT '应用名称' AFTER `app_id`;
ALTER TABLE `kb_conversation` ADD KEY `idx_app_id` (`app_id`);

-- kb_statistics表添加应用维度字段
ALTER TABLE `kb_statistics` ADD COLUMN `app_id` BIGINT DEFAULT NULL COMMENT '应用ID' AFTER `knowledge_base_id`;
ALTER TABLE `kb_statistics` ADD KEY `idx_app_id` (`app_id`);

-- 权限控制相关升级脚本
ALTER TABLE `kb_knowledge_base` ADD COLUMN `is_public` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否公开：0-仅创建者，1-所有用户可用' AFTER `status`;
ALTER TABLE `kb_knowledge_base` MODIFY COLUMN `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名';
ALTER TABLE `kb_category` MODIFY COLUMN `create_by` VARCHAR(50) DEFAULT NULL COMMENT '创建人用户名';
ALTER TABLE `im_group` ADD COLUMN `created_by` VARCHAR(50) DEFAULT NULL COMMENT '归属用户（创建者用户名）' AFTER `kb_id`;
ALTER TABLE `im_group` ADD KEY `idx_created_by` (`created_by`);


