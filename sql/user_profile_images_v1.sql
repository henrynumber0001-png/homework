-- OAuth 不再接收头像；用户头像和 Banner 统一使用私有 COS object key。
-- 执行前请备份数据库，并确认当前 user_info 尚无新增的 object key 列。
ALTER TABLE user_info
    ADD COLUMN avatar_object_key VARCHAR(512) NULL COMMENT '用户上传头像的 COS object key' AFTER avatar,
    ADD COLUMN banner_object_key VARCHAR(512) NULL COMMENT '个人中心 Banner 的 COS object key' AFTER avatar_object_key;

-- 历史 OAuth 头像不再使用；新增列成功后再删除旧字段。
ALTER TABLE user_info
    DROP COLUMN avatar;
