CREATE TABLE `user_block` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `blocker_user_id` bigint unsigned NOT NULL COMMENT '主动拉黑用户 ID',
  `blocked_user_id` bigint unsigned NOT NULL COMMENT '被拉黑用户 ID',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_block_pair` (`blocker_user_id`, `blocked_user_id`),
  KEY `idx_user_block_blocked` (`blocked_user_id`, `is_deleted`),
  CONSTRAINT `fk_user_block_blocker`
    FOREIGN KEY (`blocker_user_id`) REFERENCES `user_info` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_user_block_blocked`
    FOREIGN KEY (`blocked_user_id`) REFERENCES `user_info` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_user_block_not_self`
    CHECK (`blocker_user_id` <> `blocked_user_id`),
  CONSTRAINT `chk_user_block_deleted`
    CHECK (`is_deleted` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
