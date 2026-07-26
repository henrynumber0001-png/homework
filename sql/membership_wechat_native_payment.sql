-- 为已有 membership_order 表增加微信 Native 支付二维码内容。
-- 新建环境可直接执行 membership_v2.sql，无需再执行本迁移。

ALTER TABLE membership_order
    ADD COLUMN payment_code_url VARCHAR(2048) NULL
        COMMENT '微信 Native 预下单二维码内容'
        AFTER provider_trade_no;
