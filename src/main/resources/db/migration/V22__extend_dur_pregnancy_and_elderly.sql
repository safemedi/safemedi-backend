ALTER TABLE `dur_pregnancy`
    ADD COLUMN `drug_code` varchar(50) NOT NULL COMMENT '내부 의약품 코드 (drug_master FK)' AFTER `id`,
    ADD COLUMN `dur_product_code` varchar(50) NOT NULL COMMENT 'DUR 원본 제품코드' AFTER `drug_code`,
    ADD COLUMN `ingredient_code` varchar(50) NOT NULL COMMENT 'DUR 원본 성분코드' AFTER `drug_name`,
    MODIFY COLUMN `drug_name` varchar(255) NOT NULL COMMENT '임부 투여 제한 약품명',
    MODIFY COLUMN `grade` varchar(10) NOT NULL COMMENT '임부 금기 등급 (1, 2, M)',
    MODIFY COLUMN `notice_number` varchar(50) NULL COMMENT '심평원 고시번호',
    MODIFY COLUMN `notice_date` date NULL COMMENT '심평원 고시일자',
    MODIFY COLUMN `warning_message` text NULL COMMENT '경고 상세 정보',
    ADD INDEX `idx_dur_pregnancy_drug_code` (`drug_code`),
    ADD CONSTRAINT `uk_dur_pregnancy_product_code` UNIQUE (`dur_product_code`),
    ADD CONSTRAINT `fk_dur_pregnancy_drug_master`
        FOREIGN KEY (`drug_code`) REFERENCES `drug_master` (`drug_code`);

ALTER TABLE `dur_elderly`
    ADD COLUMN `drug_code` varchar(50) NOT NULL COMMENT '내부 의약품 코드 (drug_master FK)' AFTER `id`,
    ADD COLUMN `dur_product_code` varchar(50) NOT NULL COMMENT 'DUR 원본 제품코드' AFTER `drug_code`,
    ADD COLUMN `ingredient_code` varchar(50) NOT NULL COMMENT 'DUR 원본 성분코드' AFTER `drug_name`,
    ADD COLUMN `category` varchar(20) NOT NULL DEFAULT 'GENERAL' COMMENT '노인주의 분류 (GENERAL, NSAID)' AFTER `ingredient_code`,
    MODIFY COLUMN `drug_name` varchar(255) NOT NULL COMMENT '고령자 투여 주의 약품명',
    MODIFY COLUMN `notice_number` varchar(50) NULL COMMENT '심평원 공고번호',
    MODIFY COLUMN `notice_date` date NULL COMMENT '심평원 공고일자',
    MODIFY COLUMN `warning_message` text NULL COMMENT '경고 상세 정보',
    ADD INDEX `idx_dur_elderly_drug_code` (`drug_code`),
    ADD CONSTRAINT `uk_dur_elderly_category_product_code` UNIQUE (`category`, `dur_product_code`),
    ADD CONSTRAINT `fk_dur_elderly_drug_master`
        FOREIGN KEY (`drug_code`) REFERENCES `drug_master` (`drug_code`);
