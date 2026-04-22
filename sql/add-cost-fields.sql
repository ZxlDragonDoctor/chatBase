-- 1. kb_conversation表添加费用字段
ALTER TABLE `kb_conversation` 
ADD COLUMN `prompt_tokens` INT DEFAULT 0 COMMENT '提示词tokens' AFTER `tokens`,
ADD COLUMN `completion_tokens` INT DEFAULT 0 COMMENT '完成tokens' AFTER `prompt_tokens`,
ADD COLUMN `prompt_price` DECIMAL(10,6) DEFAULT 0 COMMENT '提示词费用(RMB)' AFTER `completion_tokens`,
ADD COLUMN `completion_price` DECIMAL(10,6) DEFAULT 0 COMMENT '完成费用(RMB)' AFTER `prompt_price`,
ADD COLUMN `total_price` DECIMAL(10,6) DEFAULT 0 COMMENT '总费用(RMB)' AFTER `completion_price`;

-- 2. kb_statistics表添加费用统计字段
ALTER TABLE `kb_statistics`
ADD COLUMN `total_prompt_tokens` BIGINT DEFAULT 0 COMMENT '总提示词tokens' AFTER `total_tokens`,
ADD COLUMN `total_completion_tokens` BIGINT DEFAULT 0 COMMENT '总完成tokens' AFTER `total_prompt_tokens`,
ADD COLUMN `total_cost` DECIMAL(12,4) DEFAULT 0 COMMENT '总费用(RMB)' AFTER `avg_latency_ms`,
ADD COLUMN `avg_cost` DECIMAL(10,6) DEFAULT 0 COMMENT '平均费用(RMB)' AFTER `total_cost`;

-- 3. 验证字段添加成功
SHOW COLUMNS FROM `kb_conversation` LIKE '%price%';
SHOW COLUMNS FROM `kb_statistics` LIKE '%cost%';