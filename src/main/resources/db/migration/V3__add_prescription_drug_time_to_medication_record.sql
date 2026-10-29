ALTER TABLE `medication_record`
    ADD COLUMN `prescription_drug_time_id` bigint COMMENT '처방 약품 복용 시간 (FK)' AFTER `prescription_id`;

ALTER TABLE `medication_record`
    ADD FOREIGN KEY (`prescription_drug_time_id`) REFERENCES `prescription_drug_time` (`id`);
