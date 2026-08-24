SET @drop_avg_correct_rate_sql = (
    SELECT IF(
        COUNT(*) > 0,
        'ALTER TABLE `question_bank` DROP COLUMN `avg_correct_rate`',
        'SELECT 1'
    )
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'question_bank'
      AND COLUMN_NAME = 'avg_correct_rate'
);

PREPARE drop_avg_correct_rate_statement FROM @drop_avg_correct_rate_sql;
EXECUTE drop_avg_correct_rate_statement;
DEALLOCATE PREPARE drop_avg_correct_rate_statement;
