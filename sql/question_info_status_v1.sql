-- 为面试题和认证题增加后台状态字段（MySQL 8.0+）。
-- 执行前请停止服务并备份数据库。

-- 先允许 status 为空，避免在历史数据完成回填前产生含义错误的默认状态。
ALTER TABLE interview_question_info
    ADD COLUMN status TINYINT NULL
        COMMENT '1 draft, 2 published, 3 offline, 4 deleted'
        AFTER is_released;

ALTER TABLE certificate_question_info
    ADD COLUMN status TINYINT NULL
        COMMENT '1 draft, 2 published, 3 offline, 4 deleted'
        AFTER is_released;

-- 旧结构只能识别“已发布”和“已删除”。其余历史题无法区分草稿与曾经下架，统一迁移为草稿。
UPDATE interview_question_info
SET status = CASE
    WHEN is_deleted = 1 THEN 4
    WHEN is_released = 1 THEN 2
    ELSE 1
END;

UPDATE certificate_question_info
SET status = CASE
    WHEN is_deleted = 1 THEN 4
    WHEN is_released = 1 THEN 2
    ELSE 1
END;

-- 回填完成后收紧为非空字段；以后新题默认处于草稿状态。
ALTER TABLE interview_question_info
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1
        COMMENT '1 draft, 2 published, 3 offline, 4 deleted';

ALTER TABLE certificate_question_info
    MODIFY COLUMN status TINYINT NOT NULL DEFAULT 1
        COMMENT '1 draft, 2 published, 3 offline, 4 deleted';

-- 删除旧字段前校验：以下四个查询都应返回 0。
SELECT COUNT(*) AS invalid_interview_question_status
FROM interview_question_info
WHERE status NOT IN (1, 2, 3, 4);

SELECT COUNT(*) AS invalid_certificate_question_status
FROM certificate_question_info
WHERE status NOT IN (1, 2, 3, 4);

SELECT COUNT(*) AS inconsistent_interview_release_status
FROM interview_question_info
WHERE is_released <> (status = 2);

SELECT COUNT(*) AS inconsistent_certificate_release_status
FROM certificate_question_info
WHERE is_released <> (status = 2);

-- status 已经完整表达发布状态，删除重复的 is_released，避免出现两个状态来源。
-- is_deleted 继续作为 MyBatis-Plus 逻辑删除字段和有效题目序号唯一索引的技术依据。
ALTER TABLE interview_question_info
    DROP COLUMN is_released,
    ADD KEY idx_interview_bank_status_question_no
        (bank_id, is_deleted, status, question_no, id),
    ADD CONSTRAINT chk_interview_question_status_deleted
        CHECK (
            (is_deleted = 0 AND status IN (1, 2, 3))
            OR (is_deleted = 1 AND status = 4)
        );

ALTER TABLE certificate_question_info
    DROP COLUMN is_released,
    ADD KEY idx_certificate_bank_status_question_no
        (bank_id, is_deleted, status, question_no, id),
    ADD CONSTRAINT chk_certificate_question_status_deleted
        CHECK (
            (is_deleted = 0 AND status IN (1, 2, 3))
            OR (is_deleted = 1 AND status = 4)
        );

-- 最终校验：两个查询都应返回 0，确认旧字段已经删除。
SELECT COUNT(*) AS interview_is_released_column_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'interview_question_info'
  AND column_name = 'is_released';

SELECT COUNT(*) AS certificate_is_released_column_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'certificate_question_info'
  AND column_name = 'is_released';
