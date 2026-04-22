-- 修复tokens为0的历史数据（根据answer长度估算）
UPDATE kb_conversation 
SET tokens = CEIL(LENGTH(answer) / 4)
WHERE tokens = 0 AND answer IS NOT NULL AND answer != '';

-- 查看更新后的数据
SELECT id, query, LEFT(answer, 50) as answer_preview, tokens, create_time 
FROM kb_conversation 
ORDER BY create_time DESC 
LIMIT 10;