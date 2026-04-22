-- 检查统计数据问题诊断脚本

-- 1. 检查kb_conversation表数据（原始对话数据）
SELECT 
    DATE(create_time) as date,
    COUNT(*) as count,
    SUM(IFNULL(tokens, 0)) as total_tokens,
    AVG(IFNULL(tokens, 0)) as avg_tokens
FROM kb_conversation 
WHERE status = 1
GROUP BY DATE(create_time)
ORDER BY date DESC
LIMIT 10;

-- 2. 检查kb_statistics表数据（聚合统计数据）
SELECT * FROM kb_statistics ORDER BY stat_date DESC LIMIT 10;

-- 3. 检查kb_statistics表是否有记录
SELECT COUNT(*) as kb_statistics_count FROM kb_statistics;

-- 4. 检查kb_conversation表是否有tokens数据
SELECT COUNT(*) as tokens_zero_count FROM kb_conversation WHERE tokens = 0;
SELECT COUNT(*) as tokens_has_value_count FROM kb_conversation WHERE tokens > 0;

-- 5. 如果kb_statistics表无数据，手动执行聚合（复制这段执行）
INSERT INTO kb_statistics (stat_date, channel, conversation_count, message_count, user_count, total_tokens, avg_tokens, avg_latency_ms, create_time)
SELECT 
    DATE(create_time) as stat_date,
    'all' as channel,
    COUNT(*) as conversation_count,
    COUNT(*) as message_count,
    COUNT(DISTINCT user_id) as user_count,
    SUM(IFNULL(tokens, 0)) as total_tokens,
    AVG(IFNULL(tokens, 0)) as avg_tokens,
    AVG(IFNULL(latency_ms, 0)) as avg_latency_ms,
    NOW() as create_time
FROM kb_conversation
WHERE status = 1
GROUP BY DATE(create_time)
ON DUPLICATE KEY UPDATE 
    conversation_count = VALUES(conversation_count),
    message_count = VALUES(message_count),
    user_count = VALUES(user_count),
    total_tokens = VALUES(total_tokens),
    avg_tokens = VALUES(avg_tokens),
    avg_latency_ms = VALUES(avg_latency_ms);

-- 6. 验证聚合后的数据
SELECT stat_date, conversation_count, total_tokens, avg_tokens FROM kb_statistics ORDER BY stat_date DESC LIMIT 10;