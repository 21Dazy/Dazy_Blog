-- 检查数据库字符集设置
SHOW VARIABLES LIKE '%character%';
SHOW VARIABLES LIKE '%collation%';

-- 检查数据库的字符集
SELECT schema_name, default_character_set_name, default_collation_name 
FROM information_schema.schemata
WHERE schema_name = DATABASE();

-- 检查blogs表的字符集
SHOW TABLE STATUS WHERE Name = 'blogs';

-- 检查blogs表中各列的字符集
SHOW FULL COLUMNS FROM blogs;

-- 检查content列中是否有中文内容及其数量
SELECT id, title, 
       LENGTH(content) AS content_length,
       (LENGTH(content) - LENGTH(REPLACE(content, '中', ''))) / 3 +
       (LENGTH(content) - LENGTH(REPLACE(content, '国', ''))) / 3 +
       (LENGTH(content) - LENGTH(REPLACE(content, '人', ''))) / 3 +
       (LENGTH(content) - LENGTH(REPLACE(content, '的', ''))) / 3 +
       (LENGTH(content) - LENGTH(REPLACE(content, '一', ''))) / 3 +
       (LENGTH(content) - LENGTH(REPLACE(content, '是', ''))) / 3 AS chinese_char_sample_count
FROM blogs;

-- 检查空内容记录
SELECT id, title, created_at
FROM blogs
WHERE content IS NULL OR content = '';

-- 检查可能有问题的内容记录
SELECT id, title, LENGTH(content) AS content_length
FROM blogs 
WHERE content LIKE '%�%' -- 查找包含乱码字符的记录
ORDER BY content_length DESC;

-- 修复数据库字符集
ALTER DATABASE CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 修复blogs表字符集
ALTER TABLE blogs CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 修复content列的字符集，根据实际情况修改类型
ALTER TABLE blogs MODIFY content LONGTEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 查看现有连接的字符集
SELECT id, user, host, db, charset
FROM information_schema.processlist
WHERE db = DATABASE(); 