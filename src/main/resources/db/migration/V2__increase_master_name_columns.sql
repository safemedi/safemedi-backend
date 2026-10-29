ALTER TABLE `drug_master`
    MODIFY COLUMN `drug_name` varchar(1000) COMMENT '의약품 제품명';

ALTER TABLE `ingredient_master`
    MODIFY COLUMN `ingredient_name` varchar(1000) COMMENT '약품 성분명';
