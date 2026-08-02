-- 题目序号迁移（MySQL 8.0+）。
-- 执行顺序：关闭 App/Admin 服务 -> 备份数据库 -> 执行本脚本 -> 部署新代码 -> 启动服务。
-- 本脚本只修改两张题目表；question_bank.sort_order 仍是题库曝光权重，必须保留。

-- 先按现有 sort_order、id 的稳定顺序，把每个题库的有效题目规范化为 1...N。
-- 已删除题目排在有效题目之后，仅用于满足 question_no 非空约束。
UPDATE interview_question_info iq
JOIN (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY bank_id
               ORDER BY is_deleted ASC, sort_order ASC, id ASC
           ) AS normalized_question_no
    FROM (
        SELECT id, bank_id, is_deleted, sort_order
        FROM interview_question_info
    ) interview_snapshot
) ranked ON ranked.id = iq.id
SET iq.sort_order = ranked.normalized_question_no;

UPDATE certificate_question_info cq
JOIN (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY bank_id
               ORDER BY is_deleted ASC, sort_order ASC, id ASC
           ) AS normalized_question_no
    FROM (
        SELECT id, bank_id, is_deleted, sort_order
        FROM certificate_question_info
    ) certificate_snapshot
) ranked ON ranked.id = cq.id
SET cq.sort_order = ranked.normalized_question_no;

-- 删除旧排序索引，物理列改名为 question_no。
ALTER TABLE interview_question_info
    DROP INDEX idx_interview_bank_order,
    CHANGE COLUMN sort_order question_no INT NOT NULL COMMENT '题库内连续且唯一的题目序号，从1开始';

ALTER TABLE certificate_question_info
    DROP INDEX idx_certificate_bank_order,
    CHANGE COLUMN sort_order question_no INT NOT NULL COMMENT '题库内连续且唯一的题目序号，从1开始';

-- MySQL 没有局部唯一索引。生成列让唯一约束只作用于 is_deleted = 0 的有效题目；
-- 已删除题目的 active_question_no 为 NULL，而唯一索引允许存在多个 NULL。
ALTER TABLE interview_question_info
    ADD COLUMN active_question_no INT GENERATED ALWAYS AS (
        CASE WHEN is_deleted = 0 THEN question_no ELSE NULL END
    ) STORED COMMENT '仅用于有效题目序号唯一约束',
    ADD UNIQUE KEY uk_interview_bank_active_question_no (bank_id, active_question_no),
    ADD KEY idx_interview_bank_question_no (bank_id, is_deleted, question_no, id);

ALTER TABLE certificate_question_info
    ADD COLUMN active_question_no INT GENERATED ALWAYS AS (
        CASE WHEN is_deleted = 0 THEN question_no ELSE NULL END
    ) STORED COMMENT '仅用于有效题目序号唯一约束',
    ADD UNIQUE KEY uk_certificate_bank_active_question_no (bank_id, active_question_no),
    ADD KEY idx_certificate_bank_question_no (bank_id, is_deleted, question_no, id);

-- 校验：以下四个查询应分别返回 0、0、空结果、空结果。
SELECT COUNT(*) AS invalid_interview_question_no
FROM interview_question_info
WHERE is_deleted = 0 AND question_no < 1;

SELECT COUNT(*) AS invalid_certificate_question_no
FROM certificate_question_info
WHERE is_deleted = 0 AND question_no < 1;

SELECT bank_id, question_no, COUNT(*) AS duplicate_count
FROM interview_question_info
WHERE is_deleted = 0
GROUP BY bank_id, question_no
HAVING COUNT(*) > 1;

SELECT bank_id, question_no, COUNT(*) AS duplicate_count
FROM certificate_question_info
WHERE is_deleted = 0
GROUP BY bank_id, question_no
HAVING COUNT(*) > 1;
