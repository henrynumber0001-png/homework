-- Run this cleanup only when an earlier membership_v2.sql version has already
-- created the three balance-carrying columns.
--
-- Same-BillingType upgrades guarantee that the unused Standard value is applied
-- to the corresponding, more expensive Premium plan. No balance is carried
-- between billing periods.

ALTER TABLE membership_order
    DROP COLUMN available_credit_amount,
    DROP COLUMN carried_credit_amount;

ALTER TABLE membership_subscription
    DROP COLUMN credit_balance;
