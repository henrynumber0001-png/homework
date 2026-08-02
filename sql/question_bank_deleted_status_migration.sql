-- Keep historical logically deleted question banks consistent with QuestionBankStatus.DELETED = 4.
UPDATE question_bank
SET status = 4
WHERE is_deleted = 1
  AND status <> 4;

ALTER TABLE question_bank
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 2
        COMMENT '1 draft, 2 published, 3 offline, 4 deleted';
