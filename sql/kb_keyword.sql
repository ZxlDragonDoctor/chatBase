-- 关键词统计表
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

-- 创建索引优化查询
CREATE INDEX idx_keyword ON kb_keyword(keyword);