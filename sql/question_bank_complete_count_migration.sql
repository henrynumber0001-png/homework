ALTER TABLE question_bank
    RENAME COLUMN complete_user_count TO complete_count,
    MODIFY COLUMN hot_score INT
        GENERATED ALWAYS AS (view_count + complete_count * 3) STORED;
