-- 题目收藏按“用户 + 题库 + 题目”唯一（MySQL 8.0+）。
-- 本迁移会为旧收藏记录选择其所在的最小 bank_id；如果存在无法关联题库的旧数据，
-- 最后的 NOT NULL 变更会失败并停止，便于先人工处理，而不会静默删除数据。

ALTER TABLE user_favorite_question
    ADD COLUMN bank_id BIGINT UNSIGNED NULL COMMENT '收藏发生的题库 ID' AFTER user_id,
    CHANGE COLUMN save_time collected_time DATETIME(3) NOT NULL
        DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最近一次收藏时间';

UPDATE user_favorite_question ufq
JOIN (
    SELECT question_id, MIN(bank_id) AS bank_id
    FROM question_bank_question
    WHERE is_deleted = 0
    GROUP BY question_id
) mapping ON mapping.question_id = ufq.question_id
SET ufq.bank_id = mapping.bank_id
WHERE ufq.bank_id IS NULL;

ALTER TABLE user_favorite_question
    MODIFY COLUMN bank_id BIGINT UNSIGNED NOT NULL COMMENT '收藏发生的题库 ID',
    DROP INDEX uk_user_favorite_question,
    ADD UNIQUE KEY uk_user_favorite_question (user_id, bank_id, question_id),
    ADD KEY idx_ufq_bank_question (bank_id, question_id, is_deleted),
    ADD CONSTRAINT fk_ufq_bank FOREIGN KEY (bank_id)
        REFERENCES question_bank (id) ON DELETE RESTRICT ON UPDATE CASCADE;
