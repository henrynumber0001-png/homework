-- graph_info 与当前 GraphInfo 实体同步（MySQL 8.0+）。
-- 执行前请停止服务并备份数据库。

-- 旧表使用实体中已不存在的 name 区分同一业务项的多张图片；
-- 当前实体没有 name，查询也按 item_type + item_id 期望唯一结果。
ALTER TABLE graph_info
    DROP INDEX uk_graph_info_item_name,
    DROP INDEX idx_graph_info_item,
    DROP COLUMN name,
    ADD COLUMN user_id BIGINT UNSIGNED NULL COMMENT '所属用户 ID' AFTER id,
    ADD UNIQUE KEY uk_graph_info_item (item_type, item_id),
    ADD KEY idx_graph_info_user_id (user_id);

-- 校验：第一个查询应返回 1，第二个查询应返回 0。
SELECT COUNT(*) AS graph_info_user_id_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'graph_info'
  AND column_name = 'user_id';

SELECT COUNT(*) AS graph_info_name_exists
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'graph_info'
  AND column_name = 'name';
