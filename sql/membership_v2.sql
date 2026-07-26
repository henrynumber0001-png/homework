-- Premium / Premium Plus 双台账会员体系（新环境初始化脚本）。

CREATE TABLE IF NOT EXISTS membership_plan (
    id BIGINT NOT NULL AUTO_INCREMENT,
    membership_type TINYINT NOT NULL COMMENT '1=PREMIUM, 2=PREMIUM_PLUS',
    purchase_type TINYINT NOT NULL COMMENT '1=FULL, 2=DIFF',
    billing_type TINYINT NULL COMMENT '1=MONTHLY, 2=QUARTERLY, 3=YEARLY；补差为空',
    duration_months TINYINT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'CNY',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_membership_plan_sku (
        membership_type,
        purchase_type,
        duration_months,
        is_deleted
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS membership_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    action TINYINT NOT NULL COMMENT '1=FULL_PURCHASE, 2=DIFF_UPGRADE',
    to_plan_id BIGINT NOT NULL,
    membership_type TINYINT NOT NULL,
    billing_type TINYINT NULL,
    duration_months TINYINT NOT NULL,
    pay_amount DECIMAL(10, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'CNY',
    period_end DATETIME NULL,
    pay_time DATETIME NULL,
    order_status TINYINT NOT NULL COMMENT '1=PENDING, 2=PAID, 4=EXPIRED, 6=PAY_FAILED',
    provider_trade_no VARCHAR(128) NULL,
    payment_code_url VARCHAR(2048) NULL COMMENT '微信 Native 预下单二维码内容',
    idempotency_key VARCHAR(64) NOT NULL,
    payment_expired_time DATETIME NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_membership_order_no (order_no),
    UNIQUE KEY uk_membership_order_user_idempotency (user_id, idempotency_key),
    UNIQUE KEY uk_membership_order_provider_trade (provider_trade_no),
    KEY idx_membership_order_user_status (user_id, order_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS base_vip_record (
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

CREATE TABLE IF NOT EXISTS svip_record (
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

-- 六个全款套餐。
INSERT INTO membership_plan
    (membership_type, purchase_type, billing_type, duration_months, price, currency, enabled, is_deleted)
VALUES
    (1, 1, 1, 1, 99.00, 'CNY', 1, 0),
    (1, 1, 2, 3, 269.00, 'CNY', 1, 0),
    (1, 1, 3, 12, 999.00, 'CNY', 1, 0),
    (2, 1, 1, 1, 129.00, 'CNY', 1, 0),
    (2, 1, 2, 3, 349.00, 'CNY', 1, 0),
    (2, 1, 3, 12, 1199.00, 'CNY', 1, 0)
ON DUPLICATE KEY UPDATE
    billing_type = VALUES(billing_type),
    price = VALUES(price),
    currency = VALUES(currency),
    enabled = VALUES(enabled);

-- 1-11 个月补差套餐，每月固定 30 元；每个月转换 31 天基础会员时长。
INSERT INTO membership_plan
    (membership_type, purchase_type, billing_type, duration_months, price, currency, enabled, is_deleted)
VALUES
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
    price = VALUES(price),
    currency = VALUES(currency),
    enabled = VALUES(enabled);
