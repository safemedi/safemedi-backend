-- dialect: MySQL
-- noinspection SqlDialectInspection
ALTER TABLE `user_allergy`
    MODIFY COLUMN `allergy_type` ENUM('ATC_GROUP', 'INGREDIENT', 'CUSTOM', 'FOOD') COMMENT '알러지 분류';
