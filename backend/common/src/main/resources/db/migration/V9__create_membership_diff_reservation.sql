CREATE TABLE IF NOT EXISTS `membership_diff_reservation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `user_id` bigint NOT NULL,
  `reserved_days` int unsigned NOT NULL,
  `reservation_status` tinyint unsigned NOT NULL COMMENT '1=RESERVED, 2=CONSUMED, 3=RELEASED',
  `expires_time` datetime NOT NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_diff_reservation_order` (`order_id`),
  KEY `idx_membership_diff_reservation_user_status` (`user_id`, `reservation_status`, `is_deleted`),
  KEY `idx_membership_diff_reservation_status_expiry` (`reservation_status`, `expires_time`, `is_deleted`),
  CONSTRAINT `fk_membership_diff_reservation_order`
    FOREIGN KEY (`order_id`) REFERENCES `membership_order` (`id`)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `chk_membership_diff_reservation_days`
    CHECK (`reserved_days` > 0),
  CONSTRAINT `chk_membership_diff_reservation_status`
    CHECK (`reservation_status` IN (1, 2, 3)),
  CONSTRAINT `chk_membership_diff_reservation_deleted`
    CHECK (`is_deleted` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='补差升级订单的 Premium 可转换时长预占';

-- Some development databases created this table manually before Flyway V9.
-- Normalize the two numeric columns without dropping existing rows.
ALTER TABLE `membership_diff_reservation`
  MODIFY COLUMN `reserved_days` int unsigned NOT NULL,
  MODIFY COLUMN `reservation_status` tinyint unsigned NOT NULL
    COMMENT '1=RESERVED, 2=CONSUMED, 3=RELEASED';

SET @add_order_unique_index_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `membership_diff_reservation` ADD UNIQUE INDEX `uk_membership_diff_reservation_order` (`order_id`)',
    'SELECT 1'
  )
  FROM (
    SELECT INDEX_NAME
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'membership_diff_reservation'
    GROUP BY INDEX_NAME
    HAVING MIN(NON_UNIQUE) = 0
       AND COUNT(*) = 1
       AND MAX(COLUMN_NAME) = 'order_id'
  ) AS order_unique_indexes
);
PREPARE add_order_unique_index_statement FROM @add_order_unique_index_sql;
EXECUTE add_order_unique_index_statement;
DEALLOCATE PREPARE add_order_unique_index_statement;

SET @add_user_status_index_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `membership_diff_reservation` ADD INDEX `idx_membership_diff_reservation_user_status` (`user_id`, `reservation_status`, `is_deleted`)',
    'SELECT 1'
  )
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'membership_diff_reservation'
    AND INDEX_NAME = 'idx_membership_diff_reservation_user_status'
);
PREPARE add_user_status_index_statement FROM @add_user_status_index_sql;
EXECUTE add_user_status_index_statement;
DEALLOCATE PREPARE add_user_status_index_statement;

SET @add_status_expiry_index_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `membership_diff_reservation` ADD INDEX `idx_membership_diff_reservation_status_expiry` (`reservation_status`, `expires_time`, `is_deleted`)',
    'SELECT 1'
  )
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'membership_diff_reservation'
    AND INDEX_NAME = 'idx_membership_diff_reservation_status_expiry'
);
PREPARE add_status_expiry_index_statement FROM @add_status_expiry_index_sql;
EXECUTE add_status_expiry_index_statement;
DEALLOCATE PREPARE add_status_expiry_index_statement;

SET @add_order_foreign_key_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `membership_diff_reservation` ADD CONSTRAINT `fk_membership_diff_reservation_order` FOREIGN KEY (`order_id`) REFERENCES `membership_order` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT',
    'SELECT 1'
  )
  FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'membership_diff_reservation'
    AND CONSTRAINT_NAME = 'fk_membership_diff_reservation_order'
);
PREPARE add_order_foreign_key_statement FROM @add_order_foreign_key_sql;
EXECUTE add_order_foreign_key_statement;
DEALLOCATE PREPARE add_order_foreign_key_statement;

SET @add_days_check_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `membership_diff_reservation` ADD CONSTRAINT `chk_membership_diff_reservation_days` CHECK (`reserved_days` > 0)',
    'SELECT 1'
  )
  FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'membership_diff_reservation'
    AND CONSTRAINT_NAME = 'chk_membership_diff_reservation_days'
);
PREPARE add_days_check_statement FROM @add_days_check_sql;
EXECUTE add_days_check_statement;
DEALLOCATE PREPARE add_days_check_statement;

SET @add_status_check_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `membership_diff_reservation` ADD CONSTRAINT `chk_membership_diff_reservation_status` CHECK (`reservation_status` IN (1, 2, 3))',
    'SELECT 1'
  )
  FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'membership_diff_reservation'
    AND CONSTRAINT_NAME = 'chk_membership_diff_reservation_status'
);
PREPARE add_status_check_statement FROM @add_status_check_sql;
EXECUTE add_status_check_statement;
DEALLOCATE PREPARE add_status_check_statement;

SET @add_deleted_check_sql = (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE `membership_diff_reservation` ADD CONSTRAINT `chk_membership_diff_reservation_deleted` CHECK (`is_deleted` IN (0, 1))',
    'SELECT 1'
  )
  FROM information_schema.TABLE_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA = DATABASE()
    AND TABLE_NAME = 'membership_diff_reservation'
    AND CONSTRAINT_NAME = 'chk_membership_diff_reservation_deleted'
);
PREPARE add_deleted_check_statement FROM @add_deleted_check_sql;
EXECUTE add_deleted_check_statement;
DEALLOCATE PREPARE add_deleted_check_statement;
