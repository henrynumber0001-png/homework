-- Hit 学习打卡及“我的消息”功能表结构（MySQL 8.0+）。
-- 首次部署时执行本文件；所有表均使用逻辑删除字段，与项目 BaseEntity 保持一致。

CREATE TABLE IF NOT EXISTS hit_post (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '发布者用户 ID',
    content VARCHAR(560) NOT NULL COMMENT 'Hit 正文，应用层限制 140 个 Unicode 字符',
    tags_json JSON NULL COMMENT '标签 JSON 数组',
    post_status TINYINT NOT NULL DEFAULT 1 COMMENT '1 published, 2 hidden, 3 deleted',
    comment_count INT UNSIGNED NOT NULL DEFAULT 0,
    like_count INT UNSIGNED NOT NULL DEFAULT 0,
    favorite_count INT UNSIGNED NOT NULL DEFAULT 0,
    repost_count INT UNSIGNED NOT NULL DEFAULT 0,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_hit_post_timeline (post_status, is_deleted, created_time DESC, id DESC),
    KEY idx_hit_post_user (user_id, is_deleted, created_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Hit 学习打卡动态';

CREATE TABLE IF NOT EXISTS hit_comment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    post_id BIGINT NOT NULL COMMENT '所属 Hit ID',
    user_id BIGINT NOT NULL COMMENT '评论者用户 ID',
    parent_id BIGINT NULL COMMENT '被回复评论 ID，顶级评论为空',
    content VARCHAR(2000) NOT NULL COMMENT '评论正文，应用层限制 500 个 Unicode 字符',
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_hit_comment_post (post_id, is_deleted, created_time, id),
    KEY idx_hit_comment_parent (parent_id, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Hit 评论';

CREATE TABLE IF NOT EXISTS hit_action (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    post_id BIGINT NOT NULL COMMENT 'Hit ID',
    user_id BIGINT NOT NULL COMMENT '互动用户 ID',
    action_type TINYINT NOT NULL COMMENT '1 like, 2 favorite, 3 repost',
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_hit_action_user (post_id, user_id, action_type),
    KEY idx_hit_action_user (user_id, is_deleted, post_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Hit 点赞、收藏、转发';

CREATE TABLE IF NOT EXISTS user_notification (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    receiver_user_id BIGINT NOT NULL COMMENT '通知接收者',
    sender_user_id BIGINT NULL COMMENT '触发通知的用户，系统公告可为空',
    notification_type TINYINT NOT NULL COMMENT '1 reply, 2 like, 3 system, 4 private message, 5 favorite, 6 repost, 7 follow',
    target_type TINYINT NOT NULL COMMENT '1 hit_post, 2 hit_comment, 3 question, 4 bank, 5 user, 6 private_message',
    target_id BIGINT NULL COMMENT '跳转目标 ID',
    title VARCHAR(100) NOT NULL,
    content VARCHAR(2000) NULL,
    read_status TINYINT NOT NULL DEFAULT 1 COMMENT '1 unread, 2 read',
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_notification_inbox (receiver_user_id, is_deleted, read_status, notification_type, created_time DESC),
    KEY idx_notification_action (receiver_user_id, sender_user_id, notification_type, target_type, target_id, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户通知';

CREATE TABLE IF NOT EXISTS private_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    sender_user_id BIGINT NOT NULL,
    receiver_user_id BIGINT NOT NULL,
    content VARCHAR(4000) NOT NULL COMMENT '纯文本，应用层限制 1000 个 Unicode 字符',
    message_status TINYINT NOT NULL DEFAULT 1 COMMENT '1 sent, 2 read, 3 blocked',
    allow_reason TINYINT NOT NULL COMMENT '1 mutual follow, 2 first non-mutual message',
    -- 仅为非互关首条消息生成唯一值；互关消息为 NULL，因此不限制正常聊天数量。
    first_non_mutual_key VARCHAR(64)
        GENERATED ALWAYS AS (
            CASE WHEN allow_reason = 2
                 THEN CONCAT(sender_user_id, ':', receiver_user_id)
                 ELSE NULL END
        ) STORED,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_private_first_non_mutual (first_non_mutual_key),
    KEY idx_private_message_sender (sender_user_id, is_deleted, created_time DESC),
    KEY idx_private_message_receiver (receiver_user_id, is_deleted, created_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户私信';

CREATE TABLE IF NOT EXISTS user_follow (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    follower_user_id BIGINT NOT NULL COMMENT '关注者',
    following_user_id BIGINT NOT NULL COMMENT '被关注者',
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_follow (follower_user_id, following_user_id),
    KEY idx_user_follow_following (following_user_id, is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户关注关系';
