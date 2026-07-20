-- Membership v2: one current subscription per user.
-- Run this migration before deploying code that reads membership_* tables.
-- The legacy premium_* tables are intentionally retained for data audit/migration.

CREATE TABLE IF NOT EXISTS membership_plan (
    id BIGINT NOT NULL AUTO_INCREMENT,
    membership_type TINYINT NOT NULL COMMENT '1=STANDARD, 2=PREMIUM',
    billing_type TINYINT NOT NULL COMMENT '1=MONTHLY, 2=YEARLY',
    price DECIMAL(10, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'CNY',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_membership_plan_type_billing (
        membership_type,
        billing_type,
        is_deleted
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS membership_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    order_no VARCHAR(32) NOT NULL,
    action TINYINT NOT NULL COMMENT '1=PURCHASE, 2=UPGRADE, 3=RENEWAL',
    from_plan_id BIGINT NULL,
    to_plan_id BIGINT NOT NULL,
    membership_type TINYINT NOT NULL,
    billing_type TINYINT NOT NULL,
    original_amount DECIMAL(10, 2) NOT NULL,
    credit_amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    pay_amount DECIMAL(10, 2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'CNY',
    period_start DATETIME NULL,
    period_end DATETIME NULL,
    pay_type TINYINT NOT NULL COMMENT '1=WECHAT, 2=ALIPAY',
    pay_time DATETIME NULL,
    order_status TINYINT NOT NULL COMMENT '1=PENDING, 2=PAID, ...',
    provider_trade_no VARCHAR(128) NULL,
    payment_code_url VARCHAR(2048) NULL COMMENT '微信 Native 预下单二维码内容',
    idempotency_key VARCHAR(64) NOT NULL,
    payment_expired_time DATETIME NOT NULL,
    source_subscription_version BIGINT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_membership_order_no (order_no),
    UNIQUE KEY uk_membership_order_user_idempotency (user_id, idempotency_key),
    UNIQUE KEY uk_membership_order_provider_trade (provider_trade_no),
    KEY idx_membership_order_user_status (user_id, order_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS membership_subscription (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    membership_type TINYINT NOT NULL,
    billing_type TINYINT NOT NULL,
    status TINYINT NOT NULL COMMENT '1=ACTIVE, 2=EXPIRED, 3=CANCELLED',
    current_period_start DATETIME NOT NULL,
    current_period_end DATETIME NOT NULL,
    current_period_amount DECIMAL(10, 2) NOT NULL,
    latest_paid_order_id BIGINT NOT NULL,
    pending_plan_id BIGINT NULL,
    pending_change_time DATETIME NULL,
    pending_order_id BIGINT NULL,
    auto_renew TINYINT(1) NOT NULL DEFAULT 1,
    subscription_version BIGINT NOT NULL DEFAULT 1,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_membership_subscription_user (user_id),
    KEY idx_membership_subscription_period_end (status, current_period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Initial product catalogue. Standard yearly currently uses 49 * 12 with no
-- annual discount because no separate yearly Standard price has been specified.
-- Confirm all four prices before production release.
INSERT INTO membership_plan
    (membership_type, billing_type, price, currency, enabled, is_deleted)
VALUES
    (1, 1, 49.00, 'CNY', 1, 0),
    (1, 2, 588.00, 'CNY', 1, 0),
    (2, 1, 79.00, 'CNY', 1, 0),
    (2, 2, 899.00, 'CNY', 1, 0)
ON DUPLICATE KEY UPDATE
    price = VALUES(price),
    currency = VALUES(currency),
    enabled = VALUES(enabled);

-- The v2 application no longer reads question_bank.is_premium because both
-- Standard and Premium can access Interview and Certificate banks. Drop that
-- legacy column in a later cleanup migration after all old application versions
-- have been retired.
