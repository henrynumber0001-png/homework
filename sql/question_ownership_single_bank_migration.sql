-- 0730 题目归属模型迁移（MySQL 8.0+）。
-- 变更说明：题目原来通过 question_bank_question 多对多关联题库；
-- 现在每道题直接在题目表保存 bank_id，并在题目表保存手动顺序 sort_order。
-- 执行前必须备份数据库，并在维护窗口内依次执行“检查、迁移、校验、收尾”。

-- 一、迁移前检查：以下查询必须全部返回空结果。
-- 如果同一道题关联多个题库，不能直接迁移，必须先复制题目并处理答题、收藏、笔记和考试数据。
SELECT cg.group_type, qbq.question_id, COUNT(DISTINCT qbq.bank_id) AS bank_count
FROM question_bank_question qbq
JOIN question_bank qb ON qb.id = qbq.bank_id
JOIN category_sub_module csm ON csm.id = qb.sub_module_id
JOIN category_module cm ON cm.id = csm.module_id
JOIN category_group cg ON cg.id = cm.group_id
WHERE qbq.is_deleted = 0
GROUP BY cg.group_type, qbq.question_id
HAVING COUNT(DISTINCT qbq.bank_id) > 1;

-- 变更说明：面试题表原名 question_info，现在改为语义明确的 interview_question_info。
-- 如果数据库已人工完成表改名，则输出提示并继续；后面的 DDL 仍保证整个脚本只能执行一次。
SET @has_old_interview_table = (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = 'question_info'
);
SET @rename_interview_table_sql = IF(
    @has_old_interview_table = 1,
    'RENAME TABLE question_info TO interview_question_info',
    'SELECT ''interview_question_info 已完成改名'' AS migration_note'
);
PREPARE rename_interview_table_statement FROM @rename_interview_table_sql;
EXECUTE rename_interview_table_statement;
DEALLOCATE PREPARE rename_interview_table_statement;

-- 变更说明：把 question_bank.priority 物理列同步改名为 sort_order。
-- 如果数据库已人工完成列改名，则直接规范现有 sort_order；两种情况都先把历史 NULL 修正为10。
SET @has_question_bank_priority = (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'question_bank'
      AND column_name = 'priority'
);
SET @fill_question_bank_sort_order_sql = IF(
    @has_question_bank_priority = 1,
    'UPDATE question_bank SET priority = 10 WHERE priority IS NULL',
    'UPDATE question_bank SET sort_order = 10 WHERE sort_order IS NULL'
);
PREPARE fill_question_bank_sort_order_statement FROM @fill_question_bank_sort_order_sql;
EXECUTE fill_question_bank_sort_order_statement;
DEALLOCATE PREPARE fill_question_bank_sort_order_statement;

SET @normalize_question_bank_sort_order_sql = IF(
    @has_question_bank_priority = 1,
    'ALTER TABLE question_bank CHANGE COLUMN priority sort_order INT NOT NULL DEFAULT 10 COMMENT ''人工曝光权重，数值越大越优先''',
    'ALTER TABLE question_bank MODIFY COLUMN sort_order INT NOT NULL DEFAULT 10 COMMENT ''人工曝光权重，数值越大越优先'''
);
PREPARE normalize_question_bank_sort_order_statement FROM @normalize_question_bank_sort_order_sql;
EXECUTE normalize_question_bank_sort_order_statement;
DEALLOCATE PREPARE normalize_question_bank_sort_order_statement;

-- 变更说明：先允许 bank_id 为空，以便安全回填；校验完成后再改为 NOT NULL。
ALTER TABLE interview_question_info
    ADD COLUMN bank_id BIGINT UNSIGNED NULL COMMENT '所属题库 ID' AFTER id;

ALTER TABLE certificate_question_info
    ADD COLUMN bank_id BIGINT UNSIGNED NULL COMMENT '所属题库 ID' AFTER id;

-- 变更说明：先修正可能存在的历史 NULL，再收紧题目顺序字段，避免 DDL 因旧数据失败。
UPDATE interview_question_info SET sort_order = 10 WHERE sort_order IS NULL;
UPDATE certificate_question_info SET sort_order = 10 WHERE sort_order IS NULL;

ALTER TABLE interview_question_info
    MODIFY COLUMN sort_order INT NOT NULL DEFAULT 10 COMMENT '题库内手动顺序，数值越小越靠前';

ALTER TABLE certificate_question_info
    MODIFY COLUMN sort_order INT NOT NULL DEFAULT 10 COMMENT '题库内手动顺序，数值越小越靠前';

-- 变更说明：将旧关系表中的题库归属和题目顺序回填到面试题表。
UPDATE interview_question_info iq
JOIN question_bank_question qbq
    ON qbq.question_id = iq.id AND qbq.is_deleted = 0
JOIN question_bank qb ON qb.id = qbq.bank_id
JOIN category_sub_module csm ON csm.id = qb.sub_module_id
JOIN category_module cm ON cm.id = csm.module_id
JOIN category_group cg ON cg.id = cm.group_id AND cg.group_type = 1
SET iq.bank_id = qbq.bank_id,
    iq.sort_order = COALESCE(qbq.sort_order, 10);

-- 变更说明：将旧关系表中的题库归属和题目顺序回填到认证题表。
UPDATE certificate_question_info cq
JOIN question_bank_question qbq
    ON qbq.question_id = cq.id AND qbq.is_deleted = 0
JOIN question_bank qb ON qb.id = qbq.bank_id
JOIN category_sub_module csm ON csm.id = qb.sub_module_id
JOIN category_module cm ON cm.id = csm.module_id
JOIN category_group cg ON cg.id = cm.group_id AND cg.group_type = 2
SET cq.bank_id = qbq.bank_id,
    cq.sort_order = COALESCE(qbq.sort_order, 10);

-- 二、迁移后校验：以下两个查询必须都返回 0。
SELECT COUNT(*) AS interview_question_without_bank
FROM interview_question_info
WHERE bank_id IS NULL;

SELECT COUNT(*) AS certificate_question_without_bank
FROM certificate_question_info
WHERE bank_id IS NULL;

-- 三、确认校验通过后收紧约束，并建立列表筛选和排序索引。
ALTER TABLE interview_question_info
    MODIFY COLUMN bank_id BIGINT UNSIGNED NOT NULL COMMENT '所属题库 ID',
    ADD KEY idx_interview_bank_updated (bank_id, is_deleted, updated_time, id),
    ADD KEY idx_interview_bank_order (bank_id, is_deleted, sort_order, id),
    ADD CONSTRAINT fk_interview_question_bank
        FOREIGN KEY (bank_id) REFERENCES question_bank (id)
        ON DELETE RESTRICT ON UPDATE CASCADE;

ALTER TABLE certificate_question_info
    MODIFY COLUMN bank_id BIGINT UNSIGNED NOT NULL COMMENT '所属题库 ID',
    ADD KEY idx_certificate_bank_updated (bank_id, is_deleted, updated_time, id),
    ADD KEY idx_certificate_bank_order (bank_id, is_deleted, sort_order, id),
    ADD CONSTRAINT fk_certificate_question_bank
        FOREIGN KEY (bank_id) REFERENCES question_bank (id)
        ON DELETE RESTRICT ON UPDATE CASCADE;

-- 四、维护窗口中确认前述校验通过后删除旧表，再部署并启动本次配套的新版本 App 与 Admin。
-- 变更说明：题目归属已经完全下沉到题目表，旧多对多关系表不再使用。
DROP TABLE question_bank_question;
