-- 题目类型字段注释对齐（MySQL 8.0+）。
-- QuestionInfoQuestionType 当前仅包含：1.single_choice、2.multiple、3.essay。
-- 本脚本只修正字段注释，不改变字段类型、数据或索引。

ALTER TABLE certificate_question_info
    MODIFY COLUMN question_type TINYINT NOT NULL
        COMMENT '1.single_choice;2.multiple;3.essay';

ALTER TABLE interview_question_info
    MODIFY COLUMN question_type TINYINT NOT NULL
        COMMENT '1.single_choice;2.multiple;3.essay';

ALTER TABLE user_question_answer
    MODIFY COLUMN question_type TINYINT NOT NULL
        COMMENT '1.single_choice;2.multiple;3.essay';

-- 校验：以下查询应返回三行，且 column_comment 均与当前枚举一致。
SELECT table_name, column_name, column_type, is_nullable, column_comment
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name IN (
      'certificate_question_info',
      'interview_question_info',
      'user_question_answer'
  )
  AND column_name = 'question_type'
ORDER BY table_name;
