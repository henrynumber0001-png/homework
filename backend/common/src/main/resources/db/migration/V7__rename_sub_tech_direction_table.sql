ALTER TABLE `tech_sub_direction`
  RENAME TO `sub_tech_direction`,
  DROP FOREIGN KEY `fk_tech_sub_direction_direction`,
  DROP CHECK `chk_tech_sub_direction_deleted`,
  RENAME INDEX `uk_tech_sub_direction_name_deleted` TO `uk_sub_tech_direction_name_deleted`,
  RENAME INDEX `idx_tech_sub_direction_direction` TO `idx_sub_tech_direction_direction`,
  ADD CONSTRAINT `fk_sub_tech_direction_direction`
    FOREIGN KEY (`direction_id`) REFERENCES `tech_direction` (`id`)
    ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT `chk_sub_tech_direction_deleted`
    CHECK (`is_deleted` IN (0, 1));
