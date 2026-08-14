CREATE TABLE `tech_direction` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `tech_direction_name` varchar(50) NOT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tech_direction_name_deleted` (`tech_direction_name`, `is_deleted`),
  CONSTRAINT `chk_tech_direction_deleted` CHECK (`is_deleted` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `tech_sub_direction` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `direction_id` bigint unsigned NOT NULL,
  `sub_direction_name` varchar(50) NOT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tech_sub_direction_name_deleted` (`direction_id`, `sub_direction_name`, `is_deleted`),
  KEY `idx_tech_sub_direction_direction` (`direction_id`, `is_deleted`),
  CONSTRAINT `fk_tech_sub_direction_direction`
    FOREIGN KEY (`direction_id`) REFERENCES `tech_direction` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_tech_sub_direction_deleted` CHECK (`is_deleted` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO `tech_direction` (`id`, `tech_direction_name`) VALUES
  (1, '后端开发'),
  (2, '前端开发'),
  (3, '移动开发'),
  (4, '测试'),
  (5, '运维'),
  (6, 'AI算法'),
  (7, '大数据');

INSERT INTO `tech_sub_direction` (`id`, `direction_id`, `sub_direction_name`) VALUES
  (101, 1, 'Java后端'),
  (102, 1, 'C++后端'),
  (103, 1, 'PHP后端'),
  (104, 1, 'Python后端'),
  (105, 1, '.NET后端'),
  (106, 1, 'Golang后端'),
  (107, 1, 'Node.js后端'),
  (201, 2, '网站开发'),
  (202, 2, '小程序开发'),
  (203, 2, 'H5开发'),
  (204, 2, '跨端开发'),
  (301, 3, 'H5开发'),
  (302, 3, 'Android开发'),
  (303, 3, 'iOS开发'),
  (304, 3, '鸿蒙开发'),
  (401, 4, '自动化测试'),
  (402, 4, '硬件测试'),
  (403, 4, '游戏测试'),
  (404, 4, '渗透测试'),
  (501, 5, '系统运维'),
  (502, 5, '技术支持'),
  (503, 5, '网络安全'),
  (504, 5, '云计算'),
  (505, 5, '实施工程师'),
  (601, 6, 'ML'),
  (602, 6, 'NLP'),
  (603, 6, '计算机视觉'),
  (604, 6, 'LLM算法'),
  (701, 7, '数据分析'),
  (702, 7, '数据挖掘'),
  (703, 7, '大数据开发');

ALTER TABLE `user_info`
  ADD COLUMN `sub_tech_direction_id` bigint unsigned DEFAULT NULL AFTER `status`,
  ADD COLUMN `company_or_school` varchar(50) DEFAULT NULL AFTER `sub_tech_direction_id`,
  ADD COLUMN `gender` tinyint DEFAULT NULL AFTER `company_or_school`,
  ADD COLUMN `introduction` varchar(100) DEFAULT NULL AFTER `gender`,
  ADD KEY `idx_user_info_sub_tech_direction` (`sub_tech_direction_id`),
  ADD CONSTRAINT `fk_user_info_sub_tech_direction`
    FOREIGN KEY (`sub_tech_direction_id`) REFERENCES `tech_sub_direction` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `chk_user_info_gender`
    CHECK (`gender` IS NULL OR `gender` IN (1, 2));
