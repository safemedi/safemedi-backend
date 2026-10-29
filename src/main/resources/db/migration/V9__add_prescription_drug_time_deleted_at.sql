ALTER TABLE `prescription_drug_time`
    ADD COLUMN `deleted_at` datetime NULL COMMENT '복용 시간 삭제 처리 일시';
