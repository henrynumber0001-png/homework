-- 已部署旧 membership_v2.sql 的环境执行一次本迁移。
-- 旧 membership_subscription 的数据迁移到双台账后删除。

ALTER TABLE membership_plan
    DROP INDEX uk_membership_plan_type_billing,
    ADD COLUMN purchase_type TINYINT NOT NULL DEFAULT 1
        COMMENT '1=FULL, 2=DIFF' AFTER membership_type,
    ADD COLUMN duration_months TINYINT NULL AFTER billing_type,
    MODIFY COLUMN billing_type TINYINT NULL
        COMMENT '1=MONTHLY, 2=QUARTERLY, 3=YEARLY；补差为空';

-- 旧枚举 2 表示 YEARLY；先保存 12 个月，再迁移为新枚举 3。
UPDATE membership_plan
SET duration_months = CASE billing_type WHEN 1 THEN 1 WHEN 2 THEN 12 END,
    billing_type = CASE billing_type WHEN 2 THEN 3 ELSE billing_type END;

ALTER TABLE membership_plan
    MODIFY COLUMN duration_months TINYINT NOT NULL,
    ADD UNIQUE KEY uk_membership_plan_sku (
        membership_type,
        purchase_type,
        duration_months,
        is_deleted
    );

ALTER TABLE membership_order
    ADD COLUMN duration_months TINYINT NULL AFTER billing_type;

UPDATE membership_order
SET duration_months = CASE billing_type WHEN 1 THEN 1 WHEN 2 THEN 12 END,
    billing_type = CASE billing_type WHEN 2 THEN 3 ELSE billing_type END;

ALTER TABLE membership_order
    MODIFY COLUMN action TINYINT NOT NULL
        COMMENT '1=FULL_PURCHASE, 2=DIFF_UPGRADE',
    MODIFY COLUMN billing_type TINYINT NULL,
    MODIFY COLUMN duration_months TINYINT NOT NULL,
    DROP COLUMN from_plan_id,
    DROP COLUMN original_amount,
    DROP COLUMN credit_amount,
    DROP COLUMN period_start,
    DROP COLUMN pay_type,
    DROP COLUMN source_subscription_version;

CREATE TABLE base_vip_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    expire_time DATETIME NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_base_vip_record_user (user_id),
    KEY idx_base_vip_record_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE svip_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    expire_time DATETIME NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_svip_record_user (user_id),
    KEY idx_svip_record_expire (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 旧体系每个用户只有一个当前套餐，因此只能迁移当时仍保存的那一级权益。
INSERT INTO base_vip_record (user_id, expire_time, created_time, updated_time, is_deleted)
SELECT user_id, current_period_end, created_time, updated_time, 0
FROM membership_subscription
WHERE membership_type = 1 AND is_deleted = 0;

INSERT INTO svip_record (user_id, expire_time, created_time, updated_time, is_deleted)
SELECT user_id, current_period_end, created_time, updated_time, 0
FROM membership_subscription
WHERE membership_type = 2 AND is_deleted = 0;

-- 套餐价格与补差 SKU 与新环境初始化脚本保持一致。
INSERT INTO membership_plan
    (membership_type, purchase_type, billing_type, duration_months, price, currency, enabled, is_deleted)
VALUES
    (1, 1, 1, 1, 99.00, 'CNY', 1, 0),
    (1, 1, 2, 3, 269.00, 'CNY', 1, 0),
    (1, 1, 3, 12, 999.00, 'CNY', 1, 0),
    (2, 1, 1, 1, 129.00, 'CNY', 1, 0),
    (2, 1, 2, 3, 349.00, 'CNY', 1, 0),
    (2, 1, 3, 12, 1199.00, 'CNY', 1, 0),
    (2, 2, NULL, 1, 30.00, 'CNY', 1, 0),
    (2, 2, NULL, 2, 60.00, 'CNY', 1, 0),
    (2, 2, NULL, 3, 90.00, 'CNY', 1, 0),
    (2, 2, NULL, 4, 120.00, 'CNY', 1, 0),
    (2, 2, NULL, 5, 150.00, 'CNY', 1, 0),
    (2, 2, NULL, 6, 180.00, 'CNY', 1, 0),
    (2, 2, NULL, 7, 210.00, 'CNY', 1, 0),
    (2, 2, NULL, 8, 240.00, 'CNY', 1, 0),
    (2, 2, NULL, 9, 270.00, 'CNY', 1, 0),
    (2, 2, NULL, 10, 300.00, 'CNY', 1, 0),
    (2, 2, NULL, 11, 330.00, 'CNY', 1, 0)
ON DUPLICATE KEY UPDATE
    billing_type = VALUES(billing_type),
    price = VALUES(price),
    currency = VALUES(currency),
    enabled = VALUES(enabled);

DROP TABLE membership_subscription;
