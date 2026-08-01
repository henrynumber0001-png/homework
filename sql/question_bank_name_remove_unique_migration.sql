-- 移除题库名称的数据库唯一约束。
-- 题库名称重复规则改由 AdminQuestionBankService 校验：
-- 同一 SubModule 中的未删除题库不能重名，不同 SubModule 可以重名。

ALTER TABLE question_bank
    DROP INDEX uk_question_bank_name_deleted;
