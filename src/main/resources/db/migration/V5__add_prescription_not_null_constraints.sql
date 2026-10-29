DELETE FROM `medication_record`
WHERE `prescription_drug_time_id` IS NULL;

ALTER TABLE `medication_record`
    MODIFY COLUMN `prescription_drug_time_id` bigint NOT NULL COMMENT '처방 약품 복용 시간 (FK)';

ALTER TABLE `prescription_drug`
    MODIFY COLUMN `drug_name` varchar(1000) NOT NULL COMMENT '약품명 (스냅샷)';
