ALTER TABLE `prescription`
    ADD COLUMN `deleted_at` datetime NULL COMMENT '처방전 삭제 처리 일시' AFTER `updated_at`;
