-- 移除题库名称的数据库唯一约束。
-- 题库名称重复规则改由 AdminQuestionBankService 校验：
-- 同一 SubModule 中的未删除题库不能重名，不同 SubModule 可以重名。

-- 旧数据库使用 uk_question_bank_name，新一点的数据库可能使用
-- uk_question_bank_name_deleted。分别检查并删除，已经不存在时安全跳过。
SET @drop_bank_name_unique_sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'question_bank'
          AND index_name = 'uk_question_bank_name'
    ),
    'ALTER TABLE question_bank DROP INDEX uk_question_bank_name',
    'SELECT ''uk_question_bank_name 已不存在'' AS migration_note'
);
PREPARE drop_bank_name_unique_statement FROM @drop_bank_name_unique_sql;
EXECUTE drop_bank_name_unique_statement;
DEALLOCATE PREPARE drop_bank_name_unique_statement;

SET @drop_bank_name_deleted_unique_sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'question_bank'
          AND index_name = 'uk_question_bank_name_deleted'
    ),
    'ALTER TABLE question_bank DROP INDEX uk_question_bank_name_deleted',
    'SELECT ''uk_question_bank_name_deleted 已不存在'' AS migration_note'
);
PREPARE drop_bank_name_deleted_unique_statement FROM @drop_bank_name_deleted_unique_sql;
EXECUTE drop_bank_name_deleted_unique_statement;
DEALLOCATE PREPARE drop_bank_name_deleted_unique_statement;
