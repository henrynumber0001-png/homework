-- HomeWork schema baseline generated from the local MySQL 8.4 development schema.
-- Structure only: no business rows, credentials, routines, or account data are included.
-- Legacy scripts under /sql are already reflected here; new schema changes must use a higher Flyway version.

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_account` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `password_hash` varchar(255) NOT NULL,
  `display_name` varchar(100) NOT NULL,
  `role` tinyint NOT NULL COMMENT '1 super admin, 2 standard admin',
  `status` tinyint NOT NULL COMMENT '1 invited, 2 active, 3 disabled, 4 archived',
  `bank_data_scope` tinyint NOT NULL DEFAULT '2' COMMENT '1 all banks, 2 assigned banks',
  `session_version` int NOT NULL DEFAULT '0',
  `last_login_time` datetime(3) DEFAULT NULL,
  `built_in` tinyint(1) NOT NULL DEFAULT '0',
  `version` int NOT NULL DEFAULT '0',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_account_email` (`email`),
  KEY `idx_admin_account_status` (`status`,`is_deleted`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_account_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_id` bigint NOT NULL,
  `permission_code` varchar(64) NOT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_permission` (`admin_id`,`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_bank_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `admin_id` bigint NOT NULL,
  `bank_id` bigint NOT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_bank_scope` (`admin_id`,`bank_id`),
  KEY `idx_admin_bank_scope_bank` (`bank_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_invitation` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `email` varchar(255) NOT NULL,
  `display_name` varchar(100) NOT NULL,
  `token_digest` char(64) NOT NULL,
  `permissions_json` json NOT NULL,
  `bank_data_scope` tinyint NOT NULL,
  `bank_ids_json` json NOT NULL,
  `expires_time` datetime(3) NOT NULL,
  `accepted_time` datetime(3) DEFAULT NULL,
  `invited_by_admin_id` bigint NOT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_invitation_token` (`token_digest`),
  KEY `idx_admin_invitation_email` (`email`,`expires_time`,`accepted_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `request_id` varchar(64) NOT NULL,
  `operator_admin_id` bigint NOT NULL,
  `operator_name` varchar(100) NOT NULL,
  `module` varchar(40) NOT NULL,
  `action` varchar(40) NOT NULL,
  `target_type` varchar(40) NOT NULL,
  `target_id` varchar(64) DEFAULT NULL,
  `reason` varchar(500) DEFAULT NULL,
  `before_snapshot` json DEFAULT NULL,
  `after_snapshot` json DEFAULT NULL,
  `success` tinyint(1) NOT NULL,
  `failure_message` varchar(500) DEFAULT NULL,
  `ip` varchar(64) DEFAULT NULL,
  `user_agent` varchar(500) DEFAULT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_admin_log_operator_time` (`operator_admin_id`,`created_time`),
  KEY `idx_admin_log_target` (`target_type`,`target_id`),
  KEY `idx_admin_log_request` (`request_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `admin_session` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_key` char(36) NOT NULL,
  `admin_id` bigint NOT NULL,
  `expires_time` datetime(3) NOT NULL,
  `revoked_time` datetime(3) DEFAULT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_admin_session_key` (`session_key`),
  KEY `idx_admin_session_admin` (`admin_id`,`revoked_time`,`expires_time`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `session_id` bigint unsigned NOT NULL,
  `question_id` bigint unsigned DEFAULT NULL,
  `group_type` tinyint DEFAULT NULL COMMENT '1.interview;2.certification',
  `sender_type` tinyint NOT NULL COMMENT '1.user;2.ai',
  `message_content` text NOT NULL,
  `model_name` varchar(100) DEFAULT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_chat_message_session` (`session_id`,`created_time`),
  CONSTRAINT `fk_ai_chat_message_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_session` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_ai_chat_message_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ai_chat_session` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `bank_id` bigint unsigned NOT NULL,
  `group_type` tinyint NOT NULL COMMENT '1.interview;2.certification',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1.active;2.closed',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_chat_user_bank` (`user_id`,`bank_id`),
  KEY `idx_ai_chat_session_user` (`user_id`),
  KEY `fk_ai_chat_session_bank` (`bank_id`),
  CONSTRAINT `fk_ai_chat_session_bank` FOREIGN KEY (`bank_id`) REFERENCES `question_bank` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_ai_chat_session_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_ai_chat_session_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bank_stat_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `bank_id` bigint NOT NULL,
  `view_count` bigint NOT NULL DEFAULT '0',
  `complete_user_count` bigint NOT NULL DEFAULT '0',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bank_stat_date_bank` (`stat_date`,`bank_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bank_tag` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bank_id` bigint unsigned NOT NULL COMMENT '题库ID',
  `tag_name` varchar(64) NOT NULL COMMENT '题库标签名称',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bank_tag` (`bank_id`,`tag_name`),
  KEY `idx_bank_tag_bank_id` (`bank_id`,`is_deleted`),
  CONSTRAINT `fk_bank_tag_bank` FOREIGN KEY (`bank_id`) REFERENCES `question_bank` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `chk_bank_tag_is_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='题库标签';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `base_vip_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `expire_time` datetime NOT NULL,
  `version` int NOT NULL DEFAULT '0',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_base_vip_record_user` (`user_id`),
  KEY `idx_base_vip_record_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `browsing_history` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `bank_id` bigint unsigned NOT NULL,
  `group_id` bigint unsigned DEFAULT NULL,
  `module_id` bigint unsigned DEFAULT NULL,
  `answer_time` int unsigned NOT NULL DEFAULT '0' COMMENT 'Accumulated answer time in seconds.',
  `view_count` int unsigned NOT NULL DEFAULT '1',
  `last_viewed_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_browsing_history_user_bank` (`user_id`,`bank_id`),
  KEY `idx_browsing_history_bank_id` (`bank_id`),
  KEY `idx_browsing_history_group_id` (`group_id`),
  KEY `idx_browsing_history_module_id` (`module_id`),
  KEY `idx_browsing_history_last_viewed_time` (`last_viewed_time`),
  CONSTRAINT `fk_browsing_history_bank` FOREIGN KEY (`bank_id`) REFERENCES `question_bank` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_browsing_history_group` FOREIGN KEY (`group_id`) REFERENCES `category_group` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_browsing_history_module` FOREIGN KEY (`module_id`) REFERENCES `category_module` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_browsing_history_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_browsing_history_is_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category_group` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `group_name` varchar(100) NOT NULL COMMENT 'For example interview or certification.',
  `group_type` tinyint NOT NULL COMMENT '1.interview;2.certification',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_group_name` (`group_name`),
  CONSTRAINT `chk_category_group_is_deleted` CHECK ((`is_deleted` in (0,1))),
  CONSTRAINT `chk_category_group_type` CHECK ((`group_type` in (1,2)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category_module` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `group_id` bigint unsigned NOT NULL,
  `module_name` varchar(100) NOT NULL,
  `sort_order` int unsigned NOT NULL DEFAULT '0' COMMENT 'UI display order',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_category_module_group_id` (`group_id`),
  KEY `idx_category_module_group_sort` (`group_id`,`sort_order`),
  CONSTRAINT `fk_category_module_group` FOREIGN KEY (`group_id`) REFERENCES `category_group` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_category_module_is_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `category_sub_module` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `module_id` bigint unsigned NOT NULL,
  `sub_module_name` varchar(150) NOT NULL COMMENT 'For example Java, Spring Boot, MySQL, Linux, AWS Associate.',
  `sort_order` int unsigned NOT NULL DEFAULT '0' COMMENT 'UI display order',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_category_sub_module_module_id` (`module_id`),
  KEY `idx_category_sub_module_module_sort` (`module_id`,`sort_order`),
  CONSTRAINT `chk_category_sub_module_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `certificate_exam_answer` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `session_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `question_id` bigint unsigned NOT NULL,
  `chosen_options` json NOT NULL,
  `answered_at` datetime(3) NOT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_certificate_exam_answer_session_question` (`session_id`,`question_id`),
  KEY `idx_cea_user_id` (`user_id`),
  KEY `idx_cea_question_id` (`question_id`),
  CONSTRAINT `fk_cea_question` FOREIGN KEY (`question_id`) REFERENCES `certificate_question_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_cea_session` FOREIGN KEY (`session_id`) REFERENCES `certificate_exam_session` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_cea_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_cea_is_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `certificate_exam_lock` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `bank_id` bigint NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_certificate_exam_lock_user_bank` (`user_id`,`bank_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `certificate_exam_session` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `bank_id` bigint unsigned NOT NULL,
  `question_order` json NOT NULL,
  `started_at` datetime(3) NOT NULL,
  `expires_at` datetime(3) NOT NULL,
  `submitted_at` datetime(3) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1.in_progress;2.submitted;3.expired',
  `correct_count` bigint unsigned DEFAULT NULL,
  `correct_rate` decimal(5,2) DEFAULT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_ces_user_bank_status` (`user_id`,`bank_id`,`status`),
  KEY `idx_ces_bank_id` (`bank_id`),
  KEY `idx_ces_status_expires` (`status`,`expires_at`),
  CONSTRAINT `fk_ces_bank` FOREIGN KEY (`bank_id`) REFERENCES `question_bank` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_ces_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_ces_correct_rate` CHECK (((`correct_rate` is null) or ((`correct_rate` >= 0) and (`correct_rate` <= 1)))),
  CONSTRAINT `chk_ces_is_deleted` CHECK ((`is_deleted` in (0,1))),
  CONSTRAINT `chk_ces_status` CHECK ((`status` in (1,2,3))),
  CONSTRAINT `chk_ces_time` CHECK ((`expires_at` > `started_at`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `certificate_question_info` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `bank_id` bigint unsigned NOT NULL COMMENT '所属题库 ID',
  `title` varchar(5000) NOT NULL,
  `options` json DEFAULT NULL,
  `correct_answer` json DEFAULT NULL,
  `analysis` mediumtext,
  `question_type` tinyint NOT NULL COMMENT '1.single_choice;2.multiple;3.essay',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 draft, 2 published, 3 offline, 4 deleted',
  `create_user_id` bigint unsigned DEFAULT NULL,
  `create_admin_id` bigint DEFAULT NULL,
  `question_no` int NOT NULL COMMENT '题库内连续且唯一的题目序号，从1开始',
  `image_object_key` varchar(512) DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  `active_question_no` int GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `question_no` else NULL end)) STORED COMMENT '仅用于有效题目序号唯一约束',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_certificate_bank_active_question_no` (`bank_id`,`active_question_no`),
  KEY `idx_cqi_type` (`question_type`),
  KEY `idx_cqi_create_user_id` (`create_user_id`),
  KEY `idx_certificate_bank_updated` (`bank_id`,`is_deleted`,`updated_time`,`id`),
  KEY `idx_certificate_bank_question_no` (`bank_id`,`is_deleted`,`question_no`,`id`),
  KEY `idx_certificate_bank_status_question_no` (`bank_id`,`is_deleted`,`status`,`question_no`,`id`),
  CONSTRAINT `fk_certificate_question_bank` FOREIGN KEY (`bank_id`) REFERENCES `question_bank` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_cqi_create_user` FOREIGN KEY (`create_user_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `chk_certificate_question_status_deleted` CHECK ((((`is_deleted` = 0) and (`status` in (1,2,3))) or ((`is_deleted` = 1) and (`status` = 4)))),
  CONSTRAINT `chk_cqi_is_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dashboard_stat_daily` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `stat_date` date NOT NULL,
  `bank_view_count` bigint NOT NULL DEFAULT '0',
  `bank_complete_user_count` bigint NOT NULL DEFAULT '0',
  `login_user_count` bigint NOT NULL DEFAULT '0',
  `register_user_count` bigint NOT NULL DEFAULT '0',
  `posting_user_count` bigint NOT NULL DEFAULT '0',
  `premium_paid_user_count` bigint NOT NULL DEFAULT '0',
  `premium_plus_paid_user_count` bigint NOT NULL DEFAULT '0',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dashboard_stat_date` (`stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `graph_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint unsigned DEFAULT NULL COMMENT '所属用户 ID',
  `item_type` tinyint NOT NULL COMMENT '业务类型',
  `item_id` bigint NOT NULL COMMENT '业务ID',
  `url` varchar(256) NOT NULL COMMENT '图片地址',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '更新时间',
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_graph_info_item` (`item_type`,`item_id`),
  KEY `idx_graph_info_user_id` (`user_id`),
  CONSTRAINT `chk_graph_info_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='图片信息表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hit_action` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `post_id` bigint unsigned NOT NULL,
  `action_user_id` bigint unsigned NOT NULL,
  `action_type` tinyint NOT NULL COMMENT '1.like;2.favorite;3.repost',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hit_action_user_post_type` (`action_user_id`,`post_id`,`action_type`),
  KEY `idx_hit_action_post_type` (`post_id`,`action_type`),
  CONSTRAINT `fk_hit_action_post` FOREIGN KEY (`post_id`) REFERENCES `hit_post` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_hit_action_user` FOREIGN KEY (`action_user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_hit_action_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hit_comment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `post_id` bigint unsigned NOT NULL,
  `comment_user_id` bigint unsigned NOT NULL,
  `parent_comment_id` bigint unsigned DEFAULT NULL,
  `comment` varchar(500) NOT NULL,
  `like_count` int unsigned NOT NULL DEFAULT '0',
  `comment_status` tinyint NOT NULL DEFAULT '1' COMMENT '1 published, 2 hidden, 3 deleted',
  `version` int NOT NULL DEFAULT '0',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_hit_comment_post` (`post_id`,`created_time`),
  KEY `idx_hit_comment_user` (`comment_user_id`),
  KEY `fk_hit_comment_parent` (`parent_comment_id`),
  CONSTRAINT `fk_hit_comment_parent` FOREIGN KEY (`parent_comment_id`) REFERENCES `hit_comment` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_hit_comment_post` FOREIGN KEY (`post_id`) REFERENCES `hit_post` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_hit_comment_user` FOREIGN KEY (`comment_user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_hit_comment_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hit_comment_like` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `comment_id` bigint unsigned NOT NULL,
  `action_user_id` bigint unsigned NOT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_hit_comment_like` (`comment_id`,`action_user_id`),
  KEY `idx_hit_comment_like_user` (`action_user_id`,`is_deleted`,`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Comment 点赞';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `hit_post` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `post_user_id` bigint unsigned NOT NULL,
  `content` varchar(140) NOT NULL,
  `tags_json` json DEFAULT NULL COMMENT 'Hit post tags as JSON array',
  `post_status` tinyint NOT NULL DEFAULT '1' COMMENT '1.published;2.hidden;3.deleted',
  `comment_count` int unsigned NOT NULL DEFAULT '0',
  `like_count` int unsigned NOT NULL DEFAULT '0',
  `favorite_count` int unsigned NOT NULL DEFAULT '0',
  `repost_count` int unsigned NOT NULL DEFAULT '0',
  `version` int NOT NULL DEFAULT '0',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_hit_post_time` (`created_time`),
  KEY `idx_hit_post_user` (`post_user_id`,`created_time`),
  KEY `idx_hit_post_hot` (`post_status`,`like_count`,`created_time`),
  CONSTRAINT `fk_hit_post_user` FOREIGN KEY (`post_user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_hit_post_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `interview_question_info` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `bank_id` bigint unsigned NOT NULL COMMENT '所属题库 ID',
  `title` varchar(5000) NOT NULL,
  `analysis` mediumtext,
  `question_type` tinyint NOT NULL COMMENT '1.single_choice;2.multiple;3.essay',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 draft, 2 published, 3 offline, 4 deleted',
  `create_user_id` bigint unsigned DEFAULT NULL,
  `create_admin_id` bigint DEFAULT NULL,
  `question_no` int NOT NULL COMMENT '题库内连续且唯一的题目序号，从1开始',
  `image_object_key` varchar(512) DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  `active_question_no` int GENERATED ALWAYS AS ((case when (`is_deleted` = 0) then `question_no` else NULL end)) STORED COMMENT '仅用于有效题目序号唯一约束',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_interview_bank_active_question_no` (`bank_id`,`active_question_no`),
  KEY `idx_question_info_type` (`question_type`),
  KEY `idx_question_info_create_user_id` (`create_user_id`),
  KEY `idx_interview_bank_updated` (`bank_id`,`is_deleted`,`updated_time`,`id`),
  KEY `idx_interview_bank_question_no` (`bank_id`,`is_deleted`,`question_no`,`id`),
  KEY `idx_interview_bank_status_question_no` (`bank_id`,`is_deleted`,`status`,`question_no`,`id`),
  CONSTRAINT `fk_interview_question_bank` FOREIGN KEY (`bank_id`) REFERENCES `question_bank` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_question_info_create_user` FOREIGN KEY (`create_user_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `chk_interview_question_status_deleted` CHECK ((((`is_deleted` = 0) and (`status` in (1,2,3))) or ((`is_deleted` = 1) and (`status` = 4)))),
  CONSTRAINT `chk_question_info_is_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `membership_access_suspension` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `reason` varchar(500) NOT NULL,
  `admin_id` bigint NOT NULL,
  `suspended_time` datetime(3) NOT NULL,
  `resumed_time` datetime(3) DEFAULT NULL,
  `resumed_by_admin_id` bigint DEFAULT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_membership_suspension_user` (`user_id`,`resumed_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `membership_change_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `change_type` tinyint NOT NULL,
  `membership_type` tinyint DEFAULT NULL,
  `duration_months` int DEFAULT NULL,
  `before_snapshot` json NOT NULL,
  `after_snapshot` json NOT NULL,
  `reason` varchar(500) NOT NULL,
  `admin_id` bigint NOT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_membership_change_user` (`user_id`,`created_time`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `membership_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `order_no` varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL,
  `action` tinyint NOT NULL COMMENT '1=FULL_PURCHASE, 2=DIFF_UPGRADE',
  `to_plan_id` bigint NOT NULL,
  `membership_type` tinyint NOT NULL,
  `billing_type` tinyint DEFAULT NULL,
  `duration_months` tinyint NOT NULL,
  `pay_amount` decimal(10,2) NOT NULL,
  `currency` char(3) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CNY',
  `period_end` datetime DEFAULT NULL,
  `pay_time` datetime DEFAULT NULL,
  `order_status` tinyint NOT NULL COMMENT '1=PENDING, 2=PAID, ...',
  `provider_trade_no` varchar(128) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payment_code_url` varchar(2048) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '微信 Native 预下单二维码内容',
  `idempotency_key` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `payment_expired_time` datetime NOT NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_order_no` (`order_no`),
  UNIQUE KEY `uk_membership_order_user_idempotency` (`user_id`,`idempotency_key`),
  UNIQUE KEY `uk_membership_order_provider_trade` (`provider_trade_no`),
  KEY `idx_membership_order_user_status` (`user_id`,`order_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `membership_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `membership_type` tinyint NOT NULL COMMENT '1=STANDARD, 2=PREMIUM',
  `purchase_type` tinyint NOT NULL DEFAULT '1' COMMENT '1=FULL, 2=DIFF',
  `billing_type` tinyint DEFAULT NULL COMMENT '1=MONTHLY, 2=QUARTERLY, 3=YEARLY；补差为空',
  `duration_months` tinyint NOT NULL,
  `price` decimal(10,2) NOT NULL,
  `currency` char(3) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'CNY',
  `enabled` tinyint(1) NOT NULL DEFAULT '1',
  `version` int NOT NULL DEFAULT '0',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_membership_plan_sku` (`membership_type`,`purchase_type`,`duration_months`,`is_deleted`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `otp_code` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `phone_number` varchar(32) NOT NULL COMMENT 'E.164 format, for example +8613812345678.',
  `code_hash` varchar(255) NOT NULL,
  `purpose` tinyint NOT NULL COMMENT '1.login;2.register;3.bind_phone;4.reset_password',
  `status` tinyint NOT NULL COMMENT '1.active;2.consumed;3.expired;4.blocked',
  `expires_time` datetime(3) NOT NULL,
  `consumed_time` datetime(3) DEFAULT NULL,
  `attempt_count` int unsigned NOT NULL DEFAULT '0',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_otp_phone_purpose` (`phone_number`,`purpose`),
  KEY `idx_otp_expires_time` (`expires_time`),
  CONSTRAINT `chk_otp_attempt_count` CHECK ((`attempt_count` <= 10)),
  CONSTRAINT `chk_otp_is_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `private_chatbox` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_a_id` bigint unsigned NOT NULL,
  `user_b_id` bigint unsigned NOT NULL,
  `initiator_user_id` bigint unsigned NOT NULL,
  `chat_access` tinyint NOT NULL COMMENT '1 pending_reply, 2 open',
  `last_message_id` bigint unsigned DEFAULT NULL,
  `last_message_time` datetime(3) DEFAULT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_private_chatbox_users` (`user_a_id`,`user_b_id`),
  KEY `idx_private_chatbox_a` (`user_a_id`,`is_deleted`,`last_message_time` DESC),
  KEY `idx_private_chatbox_b` (`user_b_id`,`is_deleted`,`last_message_time` DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='一对一私信聊天盒';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `private_message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `chatbox_id` bigint unsigned NOT NULL,
  `sender_user_id` bigint unsigned NOT NULL,
  `receiver_user_id` bigint unsigned NOT NULL,
  `content` varchar(1000) NOT NULL,
  `message_status` tinyint NOT NULL DEFAULT '1' COMMENT '1.sent;2.read;3.blocked',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_private_message_dialog` (`sender_user_id`,`receiver_user_id`,`created_time`),
  KEY `idx_private_message_receiver` (`receiver_user_id`,`message_status`),
  KEY `idx_private_message_chatbox` (`chatbox_id`,`is_deleted`,`id` DESC),
  CONSTRAINT `fk_private_message_receiver` FOREIGN KEY (`receiver_user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_private_message_sender` FOREIGN KEY (`sender_user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_private_message_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question_ai_evaluation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `answer_id` bigint unsigned NOT NULL,
  `question_id` bigint unsigned NOT NULL,
  `user_id` bigint unsigned NOT NULL,
  `score_rate` decimal(5,2) NOT NULL,
  `accurate_comment` text,
  `innovative_comment` text,
  `missing_comment` text,
  `wrong_comment` text,
  `summary` text,
  `model_name` varchar(100) DEFAULT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ai_eval_answer` (`answer_id`),
  KEY `idx_ai_eval_user_question` (`user_id`,`question_id`),
  KEY `fk_ai_eval_question` (`question_id`),
  CONSTRAINT `fk_ai_eval_answer` FOREIGN KEY (`answer_id`) REFERENCES `user_question_answer` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_ai_eval_question` FOREIGN KEY (`question_id`) REFERENCES `interview_question_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_ai_eval_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_ai_eval_deleted` CHECK ((`is_deleted` in (0,1))),
  CONSTRAINT `chk_ai_eval_score` CHECK (((`score_rate` >= 0) and (`score_rate` <= 100)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question_bank` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `bank_name` varchar(200) NOT NULL COMMENT '题库名称',
  `sub_module_id` bigint unsigned NOT NULL COMMENT '所属 category_sub_module id',
  `complete_count` int unsigned NOT NULL DEFAULT '0' COMMENT '完成题库的人数',
  `avg_correct_rate` decimal(5,2) NOT NULL DEFAULT '0.00',
  `view_count` int unsigned NOT NULL DEFAULT '0',
  `sort_order` int NOT NULL DEFAULT '10' COMMENT '人工曝光权重，数值越大越优先',
  `create_user_id` bigint unsigned DEFAULT NULL,
  `create_admin_id` bigint DEFAULT NULL,
  `published_time` datetime(3) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '2' COMMENT '1 draft, 2 published, 3 offline',
  `delete_reason` varchar(500) DEFAULT NULL,
  `version` int NOT NULL DEFAULT '0',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  `hot_score` int GENERATED ALWAYS AS ((`view_count` + (`complete_count` * 3))) STORED,
  PRIMARY KEY (`id`),
  KEY `idx_question_bank_create_user_id` (`create_user_id`),
  KEY `idx_question_bank_sub_module_id` (`sub_module_id`),
  KEY `idx_question_bank_hot` (`complete_count`,`view_count`,`sort_order`),
  KEY `idx_question_bank_priority` (`sort_order`),
  KEY `idx_question_bank_hot_score` (`sub_module_id`,`hot_score` DESC,`id`),
  FULLTEXT KEY `ft_question_bank_bank_name` (`bank_name`) /*!50100 WITH PARSER `ngram` */ ,
  CONSTRAINT `fk_question_bank_create_user` FOREIGN KEY (`create_user_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_question_bank_sub_module` FOREIGN KEY (`sub_module_id`) REFERENCES `category_sub_module` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_question_bank_is_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question_import_error` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_id` bigint NOT NULL,
  `source_row_number` int NOT NULL,
  `field_name` varchar(100) DEFAULT NULL,
  `error_message` varchar(500) NOT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_question_import_error_task` (`task_id`,`source_row_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question_import_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `task_no` varchar(40) NOT NULL,
  `bank_id` bigint NOT NULL,
  `admin_id` bigint NOT NULL,
  `file_name` varchar(255) NOT NULL,
  `file_sha256` char(64) NOT NULL,
  `file_path` varchar(1000) NOT NULL,
  `status` tinyint NOT NULL,
  `total_rows` int DEFAULT NULL,
  `valid_rows` int DEFAULT NULL,
  `error_rows` int DEFAULT NULL,
  `imported_rows` int DEFAULT NULL,
  `failure_reason` varchar(500) DEFAULT NULL,
  `expires_time` datetime(3) NOT NULL,
  `finished_time` datetime(3) DEFAULT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_import_task_no` (`task_no`),
  KEY `idx_question_import_sha` (`admin_id`,`bank_id`,`file_sha256`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `question_option` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `question_id` bigint unsigned NOT NULL,
  `option_key` varchar(10) NOT NULL COMMENT 'A/B/C/D...',
  `option_content` text NOT NULL,
  `is_correct` tinyint(1) NOT NULL DEFAULT '0',
  `sort_order` int unsigned NOT NULL DEFAULT '0',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_question_option_key` (`question_id`,`option_key`),
  KEY `idx_question_option_question_id` (`question_id`),
  CONSTRAINT `fk_question_option_question` FOREIGN KEY (`question_id`) REFERENCES `interview_question_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_question_option_correct` CHECK ((`is_correct` in (0,1))),
  CONSTRAINT `chk_question_option_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `svip_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `expire_time` datetime NOT NULL,
  `version` int NOT NULL DEFAULT '0',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_svip_record_user` (`user_id`),
  KEY `idx_svip_record_expire` (`expire_time`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_auth_identities` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `provider` tinyint NOT NULL COMMENT '1.email_password;2.phone_otp;3.google;4.wechat;5.qq',
  `identifier` varchar(255) NOT NULL COMMENT 'Original or display login identifier: email, phone, openid.',
  `identifier_normalized` varchar(255) NOT NULL COMMENT 'Normalized identifier used for login lookup and uniqueness.',
  `password_hash` varchar(255) DEFAULT NULL,
  `status` tinyint NOT NULL COMMENT '1.pending;2.verified;3.disabled;4.unlinked',
  `verified_time` datetime(3) DEFAULT NULL,
  `last_used_time` datetime(3) DEFAULT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_provider_identifier` (`provider`,`identifier_normalized`),
  KEY `idx_auth_user_id` (`user_id`),
  KEY `idx_auth_status` (`status`),
  CONSTRAINT `fk_auth_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_auth_email_password_hash` CHECK (((`provider` <> 1) or (`password_hash` is not null))),
  CONSTRAINT `chk_auth_is_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_bank_correct_rate` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `bank_id` bigint NOT NULL,
  `group_type` tinyint NOT NULL COMMENT '题库类型：1 interview，2 certification',
  `correct_rate` decimal(5,2) NOT NULL,
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_bank_correct_rate` (`bank_id`,`is_deleted`),
  KEY `idx_user_bank_correct_rate` (`user_id`,`bank_id`,`is_deleted`,`created_time`),
  KEY `idx_group_correct_rate` (`group_type`,`is_deleted`,`created_time`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_community_restriction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `scope` tinyint NOT NULL COMMENT '1 post, 2 comment, 3 both',
  `start_time` datetime(3) NOT NULL,
  `end_time` datetime(3) DEFAULT NULL,
  `active` tinyint(1) NOT NULL DEFAULT '1',
  `reason` varchar(500) NOT NULL,
  `admin_id` bigint NOT NULL,
  `version` int NOT NULL DEFAULT '0',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_user_community_active` (`user_id`,`active`,`end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_favorite_question` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `bank_id` bigint unsigned NOT NULL COMMENT '收藏发生的题库 ID',
  `question_id` bigint unsigned NOT NULL,
  `collected_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '最近一次收藏时间',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_favorite_question` (`user_id`,`bank_id`,`question_id`),
  KEY `idx_ufq_question_id` (`question_id`),
  KEY `idx_ufq_bank_question` (`bank_id`,`question_id`,`is_deleted`),
  CONSTRAINT `fk_ufq_bank` FOREIGN KEY (`bank_id`) REFERENCES `question_bank` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_ufq_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_ufq_is_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_follow` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `follower_user_id` bigint unsigned NOT NULL,
  `followee_user_id` bigint unsigned NOT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_follow_pair` (`follower_user_id`,`followee_user_id`),
  KEY `idx_user_follow_followee` (`followee_user_id`,`is_deleted`),
  CONSTRAINT `fk_user_follow_followee` FOREIGN KEY (`followee_user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_user_follow_follower` FOREIGN KEY (`follower_user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_user_follow_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_info` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `account_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `display_name` varchar(100) NOT NULL,
  `avatar_object_key` varchar(512) DEFAULT NULL COMMENT '用户上传头像的 COS object key',
  `banner_object_key` varchar(512) DEFAULT NULL COMMENT '个人中心 Banner 的 COS object key',
  `status` tinyint NOT NULL COMMENT '1.active;2.disabled;3.banned',
  `version` int NOT NULL DEFAULT '0',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_info_account_no` (`account_no`),
  KEY `idx_user_info_status` (`status`),
  CONSTRAINT `chk_user_info_is_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_info_backup_before_profile_images_20260809` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `account_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `display_name` varchar(100) NOT NULL,
  `avatar` varchar(512) DEFAULT NULL,
  `status` tinyint NOT NULL COMMENT '1.active;2.disabled;3.banned',
  `version` int NOT NULL DEFAULT '0',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_info_account_no` (`account_no`),
  KEY `idx_user_info_status` (`status`),
  CONSTRAINT `user_info_backup_before_profile_images_20260809_chk_1` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_learning_stat_daily` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `stat_date` date NOT NULL,
  `study_seconds` bigint unsigned NOT NULL DEFAULT '0' COMMENT '当天累计学习秒数',
  `last_heartbeat_time` datetime(3) DEFAULT NULL COMMENT '后端最后一次接受心跳的时间',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_learning_date` (`user_id`,`stat_date`),
  CONSTRAINT `fk_learning_stat_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_learning_stat_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_notification` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `receiver_user_id` bigint unsigned NOT NULL,
  `sender_user_id` bigint unsigned DEFAULT NULL,
  `notification_type` tinyint NOT NULL COMMENT '1.reply;2.like;3.system;4.private_message;5.favorite;6.repost;7.follow',
  `send_to` tinyint NOT NULL COMMENT '1 hit_post, 2 hit_comment, 3 question, 4 bank, 5 user, 6 private_message',
  `item_id` bigint unsigned DEFAULT NULL COMMENT '关联对象 ID',
  `post_id` bigint unsigned DEFAULT NULL COMMENT '关联 Post，评论删除后仍可跳转',
  `title` varchar(200) NOT NULL,
  `content` varchar(1000) DEFAULT NULL,
  `read_status` tinyint NOT NULL DEFAULT '1' COMMENT '1.unread;2.read',
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_notification_receiver_type` (`receiver_user_id`,`notification_type`,`read_status`),
  KEY `idx_notification_time` (`created_time`),
  KEY `fk_notification_sender` (`sender_user_id`),
  KEY `idx_notification_post` (`post_id`,`is_deleted`),
  KEY `idx_notification_action` (`receiver_user_id`,`sender_user_id`,`notification_type`,`send_to`,`item_id`,`is_deleted`),
  CONSTRAINT `fk_notification_receiver` FOREIGN KEY (`receiver_user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_notification_sender` FOREIGN KEY (`sender_user_id`) REFERENCES `user_info` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `chk_notification_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_question_answer` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `bank_id` bigint unsigned NOT NULL,
  `question_id` bigint unsigned NOT NULL,
  `question_type` tinyint NOT NULL COMMENT '1.single_choice;2.multiple;3.essay',
  `content` text,
  `chosen_options` json DEFAULT NULL,
  `is_correct` tinyint(1) DEFAULT NULL,
  `ai_score_rate` decimal(5,2) DEFAULT NULL,
  `answered_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_bank_question` (`user_id`,`bank_id`,`question_id`),
  KEY `idx_uqa_question_id` (`question_id`),
  KEY `idx_uqa_bank_id` (`bank_id`),
  CONSTRAINT `fk_uqa_bank` FOREIGN KEY (`bank_id`) REFERENCES `question_bank` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `fk_uqa_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_uqa_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_question_note` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `bank_id` bigint unsigned DEFAULT NULL,
  `question_id` bigint unsigned NOT NULL,
  `note_content` text NOT NULL,
  `created_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_time` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `is_deleted` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_bank_question` (`user_id`,`bank_id`,`question_id`),
  KEY `idx_user_note_user_question` (`user_id`,`question_id`),
  KEY `idx_user_note_bank_id` (`bank_id`),
  KEY `fk_user_note_question` (`question_id`),
  CONSTRAINT `fk_user_note_bank` FOREIGN KEY (`bank_id`) REFERENCES `question_bank` (`id`) ON DELETE SET NULL ON UPDATE CASCADE,
  CONSTRAINT `fk_user_note_user` FOREIGN KEY (`user_id`) REFERENCES `user_info` (`id`) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT `chk_user_note_deleted` CHECK ((`is_deleted` in (0,1)))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
