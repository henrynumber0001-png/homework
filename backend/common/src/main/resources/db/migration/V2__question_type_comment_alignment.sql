-- Keep database comments aligned with QuestionInfoQuestionType.
-- This is intentionally safe after V1 and upgrades existing databases baselined at V1.

ALTER TABLE certificate_question_info
    MODIFY COLUMN question_type TINYINT NOT NULL
        COMMENT '1.single_choice;2.multiple;3.essay';

ALTER TABLE interview_question_info
    MODIFY COLUMN question_type TINYINT NOT NULL
        COMMENT '1.single_choice;2.multiple;3.essay';

ALTER TABLE user_question_answer
    MODIFY COLUMN question_type TINYINT NOT NULL
        COMMENT '1.single_choice;2.multiple;3.essay';
