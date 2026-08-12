-- UserAuthIdentity no longer stores the original identifier. The normalized
-- login value becomes the single account column used together with provider.
ALTER TABLE user_auth_identities
    DROP INDEX uk_auth_provider_identifier,
    DROP COLUMN identifier,
    CHANGE COLUMN identifier_normalized account VARCHAR(255) NOT NULL
        COMMENT 'Normalized account used for login lookup and uniqueness',
    ADD UNIQUE KEY uk_auth_provider_account (provider, account);
