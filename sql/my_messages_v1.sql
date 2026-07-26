-- “我的消息”第一版迁移（MySQL 8.0+）。执行前请备份数据库。

-- 保留原有数据，只修改通知关联对象字段的名称。
ALTER TABLE user_notification
    CHANGE COLUMN target_type send_to TINYINT NOT NULL
        COMMENT '1 hit_post, 2 hit_comment, 3 question, 4 bank, 5 user, 6 private_message',
    CHANGE COLUMN target_id item_id BIGINT NULL
        COMMENT '关联对象 ID';

ALTER TABLE user_notification
    ADD COLUMN post_id BIGINT NULL COMMENT '关联 Post，评论删除后仍可跳转' AFTER item_id,
    ADD KEY idx_notification_post (post_id, is_deleted);

UPDATE user_notification
SET post_id = item_id
WHERE send_to = 1 AND post_id IS NULL;

UPDATE user_notification n
JOIN hit_comment c ON c.id = n.item_id
SET n.post_id = c.post_id
WHERE n.send_to = 2 AND n.post_id IS NULL;

UPDATE user_notification
SET is_deleted = 1
WHERE notification_type = 4;

ALTER TABLE hit_comment
    ADD COLUMN like_count INT UNSIGNED NOT NULL DEFAULT 0 AFTER comment;

CREATE TABLE hit_comment_like (
    id BIGINT NOT NULL AUTO_INCREMENT,
    comment_id BIGINT NOT NULL,
    action_user_id BIGINT NOT NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_hit_comment_like (comment_id, action_user_id),
    KEY idx_hit_comment_like_user (action_user_id, is_deleted, comment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE private_chatbox (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_a_id BIGINT NOT NULL,
    user_b_id BIGINT NOT NULL,
    initiator_user_id BIGINT NOT NULL,
    chat_access TINYINT NOT NULL COMMENT '1 pending_reply, 2 open',
    last_message_id BIGINT NULL,
    last_message_time DATETIME(3) NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_private_chatbox_users (user_a_id, user_b_id),
    KEY idx_private_chatbox_a (user_a_id, is_deleted, last_message_time DESC),
    KEY idx_private_chatbox_b (user_b_id, is_deleted, last_message_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 先增加可空列，以下步骤完成历史 Chatbox 回填后再改为 NOT NULL。
ALTER TABLE private_message
    ADD COLUMN chatbox_id BIGINT NULL AFTER id,
    ADD KEY idx_private_message_chatbox (chatbox_id, is_deleted, id DESC);

INSERT INTO private_chatbox (
    user_a_id, user_b_id, initiator_user_id, chat_access,
    last_message_id, last_message_time
)
SELECT
    x.user_a_id,
    x.user_b_id,
    x.initiator_user_id,
    CASE
        WHEN x.sender_count > 1 OR x.had_mutual_message = 1 THEN 2
        WHEN EXISTS (
            SELECT 1 FROM user_follow f1
            JOIN user_follow f2
              ON f2.follower_user_id = x.user_b_id
             AND f2.followee_user_id = x.user_a_id
             AND f2.is_deleted = 0
            WHERE f1.follower_user_id = x.user_a_id
              AND f1.followee_user_id = x.user_b_id
              AND f1.is_deleted = 0
        ) THEN 2
        ELSE 1
    END,
    x.last_message_id,
    x.last_message_time
FROM (
    SELECT
        LEAST(sender_user_id, receiver_user_id) user_a_id,
        GREATEST(sender_user_id, receiver_user_id) user_b_id,
        CAST(SUBSTRING_INDEX(GROUP_CONCAT(sender_user_id ORDER BY id), ',', 1) AS UNSIGNED) initiator_user_id,
        COUNT(DISTINCT sender_user_id) sender_count,
        MAX(allow_reason = 1) had_mutual_message,
        MAX(id) last_message_id,
        MAX(created_time) last_message_time
    FROM private_message
    WHERE is_deleted = 0
    GROUP BY LEAST(sender_user_id, receiver_user_id),
             GREATEST(sender_user_id, receiver_user_id)
) x;

UPDATE private_message m
JOIN private_chatbox c
  ON c.user_a_id = LEAST(m.sender_user_id, m.receiver_user_id)
 AND c.user_b_id = GREATEST(m.sender_user_id, m.receiver_user_id)
SET m.chatbox_id = c.id
WHERE m.chatbox_id IS NULL;

ALTER TABLE private_message
    DROP INDEX uk_private_first_non_mutual,
    DROP COLUMN first_non_mutual_key,
    DROP COLUMN allow_reason,
    MODIFY chatbox_id BIGINT NOT NULL;
