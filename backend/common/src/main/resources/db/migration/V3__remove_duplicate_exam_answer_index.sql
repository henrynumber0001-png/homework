-- Older databases may contain two identical unique indexes on session_id/question_id.
-- The baseline keeps only the descriptive index name; this migration cleans existing schemas.

SET @drop_duplicate_exam_answer_index_sql = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'certificate_exam_answer'
          AND index_name = 'uk_cea_session_question'
    ),
    'ALTER TABLE certificate_exam_answer DROP INDEX uk_cea_session_question',
    'SELECT 1'
);

PREPARE drop_duplicate_exam_answer_index_statement
    FROM @drop_duplicate_exam_answer_index_sql;
EXECUTE drop_duplicate_exam_answer_index_statement;
DEALLOCATE PREPARE drop_duplicate_exam_answer_index_statement;
