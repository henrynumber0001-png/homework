-- Homework web-admin V1 schema.
-- Execute once before starting web-admin.

ALTER TABLE question_bank
    ADD COLUMN status TINYINT NOT NULL DEFAULT 2 COMMENT '1 draft, 2 published, 3 offline, 4 deleted' AFTER published_time,
    ADD COLUMN delete_reason VARCHAR(500) NULL AFTER status,
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER delete_reason,
    ADD COLUMN create_admin_id BIGINT NULL AFTER create_user_id;

ALTER TABLE question_info
    MODIFY COLUMN title VARCHAR(5000) NOT NULL,
    ADD COLUMN image_object_key VARCHAR(512) NULL AFTER sort_order,
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER image_object_key,
    ADD COLUMN create_admin_id BIGINT NULL AFTER create_user_id;

ALTER TABLE certificate_question_info
    MODIFY COLUMN title VARCHAR(5000) NOT NULL,
    CHANGE COLUMN image_url image_object_key VARCHAR(512) NULL,
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER image_object_key,
    ADD COLUMN create_admin_id BIGINT NULL AFTER create_user_id;

ALTER TABLE user_info
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER status;

ALTER TABLE hit_post
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER repost_count;

ALTER TABLE hit_comment
    ADD COLUMN comment_status TINYINT NOT NULL DEFAULT 1 COMMENT '1 published, 2 hidden, 3 deleted' AFTER like_count,
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER comment_status;

ALTER TABLE membership_plan
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER enabled;

ALTER TABLE base_vip_record
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER expire_time;

ALTER TABLE svip_record
    ADD COLUMN version INT NOT NULL DEFAULT 0 AFTER expire_time;

CREATE TABLE admin_account (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    role TINYINT NOT NULL COMMENT '1 super admin, 2 standard admin',
    status TINYINT NOT NULL COMMENT '1 invited, 2 active, 3 disabled, 4 archived',
    bank_data_scope TINYINT NOT NULL DEFAULT 2 COMMENT '1 all banks, 2 assigned banks',
    session_version INT NOT NULL DEFAULT 0,
    last_login_time DATETIME(3) NULL,
    built_in TINYINT(1) NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_account_email (email),
    KEY idx_admin_account_status (status, is_deleted)
);

CREATE TABLE admin_invitation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    token_digest CHAR(64) NOT NULL,
    permissions_json JSON NOT NULL,
    bank_data_scope TINYINT NOT NULL,
    bank_ids_json JSON NOT NULL,
    expires_time DATETIME(3) NOT NULL,
    accepted_time DATETIME(3) NULL,
    invited_by_admin_id BIGINT NOT NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_invitation_token (token_digest),
    KEY idx_admin_invitation_email (email, expires_time, accepted_time)
);

CREATE TABLE admin_account_permission (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    permission_code VARCHAR(64) NOT NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_permission (admin_id, permission_code)
);

CREATE TABLE admin_bank_scope (
    id BIGINT NOT NULL AUTO_INCREMENT,
    admin_id BIGINT NOT NULL,
    bank_id BIGINT NOT NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_bank_scope (admin_id, bank_id),
    KEY idx_admin_bank_scope_bank (bank_id)
);

CREATE TABLE admin_session (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_key CHAR(36) NOT NULL,
    admin_id BIGINT NOT NULL,
    expires_time DATETIME(3) NOT NULL,
    revoked_time DATETIME(3) NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_session_key (session_key),
    KEY idx_admin_session_admin (admin_id, revoked_time, expires_time)
);

CREATE TABLE admin_operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    request_id VARCHAR(64) NOT NULL,
    operator_admin_id BIGINT NOT NULL,
    operator_name VARCHAR(100) NOT NULL,
    module VARCHAR(40) NOT NULL,
    action VARCHAR(40) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_id VARCHAR(64) NULL,
    reason VARCHAR(500) NULL,
    before_snapshot JSON NULL,
    after_snapshot JSON NULL,
    success TINYINT(1) NOT NULL,
    failure_message VARCHAR(500) NULL,
    ip VARCHAR(64) NULL,
    user_agent VARCHAR(500) NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_admin_log_operator_time (operator_admin_id, created_time),
    KEY idx_admin_log_target (target_type, target_id),
    KEY idx_admin_log_request (request_id)
);

CREATE TABLE question_import_task (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_no VARCHAR(40) NOT NULL,
    bank_id BIGINT NOT NULL,
    admin_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_sha256 CHAR(64) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    status TINYINT NOT NULL,
    total_rows INT NULL,
    valid_rows INT NULL,
    error_rows INT NULL,
    imported_rows INT NULL,
    failure_reason VARCHAR(500) NULL,
    expires_time DATETIME(3) NOT NULL,
    finished_time DATETIME(3) NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_question_import_task_no (task_no),
    KEY idx_question_import_sha (admin_id, bank_id, file_sha256, status)
);

CREATE TABLE question_import_error (
    id BIGINT NOT NULL AUTO_INCREMENT,
    task_id BIGINT NOT NULL,
    source_row_number INT NOT NULL,
    field_name VARCHAR(100) NULL,
    error_message VARCHAR(500) NOT NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_question_import_error_task (task_id, source_row_number)
);

CREATE TABLE user_community_restriction (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    scope TINYINT NOT NULL COMMENT '1 post, 2 comment, 3 both',
    start_time DATETIME(3) NOT NULL,
    end_time DATETIME(3) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    reason VARCHAR(500) NOT NULL,
    admin_id BIGINT NOT NULL,
    version INT NOT NULL DEFAULT 0,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_community_active (user_id, active, end_time)
);

CREATE TABLE membership_access_suspension (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    admin_id BIGINT NOT NULL,
    suspended_time DATETIME(3) NOT NULL,
    resumed_time DATETIME(3) NULL,
    resumed_by_admin_id BIGINT NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_membership_suspension_user (user_id, resumed_time)
);

CREATE TABLE membership_change_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    change_type TINYINT NOT NULL,
    membership_type TINYINT NULL,
    duration_months INT NULL,
    before_snapshot JSON NOT NULL,
    after_snapshot JSON NOT NULL,
    reason VARCHAR(500) NOT NULL,
    admin_id BIGINT NOT NULL,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_membership_change_user (user_id, created_time)
);

CREATE TABLE dashboard_stat_daily (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stat_date DATE NOT NULL,
    bank_view_count BIGINT NOT NULL DEFAULT 0,
    bank_complete_user_count BIGINT NOT NULL DEFAULT 0,
    login_user_count BIGINT NOT NULL DEFAULT 0,
    register_user_count BIGINT NOT NULL DEFAULT 0,
    posting_user_count BIGINT NOT NULL DEFAULT 0,
    premium_paid_user_count BIGINT NOT NULL DEFAULT 0,
    premium_plus_paid_user_count BIGINT NOT NULL DEFAULT 0,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_dashboard_stat_date (stat_date)
);

CREATE TABLE bank_stat_daily (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stat_date DATE NOT NULL,
    bank_id BIGINT NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    complete_user_count BIGINT NOT NULL DEFAULT 0,
    created_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_time DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    is_deleted TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_bank_stat_date_bank (stat_date, bank_id)
);
