-- 测试数据插入脚本
-- 用于验证统计功能

-- 如果 kb_conversation 表没有数据，插入一些测试数据
INSERT INTO `kb_conversation` (`session_id`, `conversation_id`, `user_id`, `channel`, `query`, `answer`, `tokens`, `latency_ms`, `status`, `create_time`)
VALUES 
('test-session-001', 'conv-001', 'user-001', 'web', '你好', '你好！有什么可以帮助您的吗？', 50, 120, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
('test-session-001', 'conv-002', 'user-001', 'web', '天气怎么样', '今天天气晴朗，温度适宜。', 80, 150, 1, DATE_SUB(NOW(), INTERVAL 1 DAY)),
('test-session-002', 'conv-003', 'user-002', 'web', '帮我写一段代码', '好的，请问您需要什么类型的代码？', 100, 200, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
('test-session-002', 'conv-004', 'user-002', 'web', 'Python排序算法', '这里是一个Python快速排序的实现...', 200, 350, 1, DATE_SUB(NOW(), INTERVAL 2 DAY)),
('test-session-003', 'conv-005', 'user-003', 'im', '今天的新闻', '今天的主要新闻有...', 150, 180, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
('test-session-003', 'conv-006', 'user-003', 'im', '推荐一本书', '推荐您阅读《深度学习》...', 120, 160, 1, DATE_SUB(NOW(), INTERVAL 3 DAY)),
('test-session-004', 'conv-007', 'user-004', 'web', '如何学习Java', '学习Java可以从基础语法开始...', 180, 250, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
('test-session-004', 'conv-008', 'user-004', 'web', 'Spring Boot教程', 'Spring Boot是一个优秀的框架...', 220, 300, 1, DATE_SUB(NOW(), INTERVAL 4 DAY)),
('test-session-005', 'conv-009', 'user-005', 'web', '数据库设计', '数据库设计需要考虑多个方面...', 160, 220, 1, DATE_SUB(NOW(), INTERVAL 5 DAY)),
('test-session-005', 'conv-010', 'user-005', 'web', 'MySQL优化', 'MySQL优化可以从索引、查询等方面入手...', 190, 280, 1, DATE_SUB(NOW(), INTERVAL 5 DAY));

-- 手动触发聚合后，查询统计结果
-- SELECT * FROM kb_statistics ORDER BY stat_date DESC LIMIT 10;

-- 查看对话数据
-- SELECT DATE(create_time) as date, COUNT(*) as count, SUM(tokens) as total_tokens FROM kb_conversation GROUP BY DATE(create_time);

-- 查看当前表数据情况
SELECT 'kb_conversation' as table_name, COUNT(*) as count FROM kb_conversation
UNION ALL
SELECT 'kb_statistics', COUNT(*) FROM kb_statistics;