-- App 用户与后台管理员已使用独立账号体系，user_info 不再保存角色。
ALTER TABLE user_info
    DROP COLUMN user_role;
