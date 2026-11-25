-- V22는 수정하지 않는다. 기존 이름 기반 규칙은 검증 전까지 비활성 상태로 보존한다.
CREATE TABLE dur_import_batch (
    source_version varchar(100) PRIMARY KEY,
    manifest_sha256 char(64) NOT NULL
);

CREATE TABLE dur_active_batch (
    dur_type varchar(20) PRIMARY KEY,
    source_version varchar(100) NOT NULL
);

CREATE TABLE dur_source_record (
    record_key char(64) PRIMARY KEY,
    source_version varchar(100) NOT NULL,
    dur_type varchar(20) NOT NULL,
    source_sha256 char(64) NOT NULL,
    source_row int NOT NULL,
    payload longtext NOT NULL,
    INDEX idx_dur_source_version (source_version, dur_type)
);

CREATE TABLE drug_external_code_map (
    id bigint PRIMARY KEY AUTO_INCREMENT,
    external_code varchar(50) NOT NULL,
    ingredient_code varchar(50) NOT NULL,
    drug_code varchar(50) NOT NULL,
    source_version varchar(100) NOT NULL,
    mapping_method varchar(50) NOT NULL,
    evidence text NOT NULL,
    verification_status varchar(20) NOT NULL,
    UNIQUE KEY uk_drug_external_version (source_version, external_code, ingredient_code),
    FOREIGN KEY (drug_code) REFERENCES drug_master (drug_code)
);

-- 약품 식별 검증과 내부 코드 단위 판정 승인을 구분한다.
CREATE TABLE dur_coverage (
    coverage_key varchar(100) PRIMARY KEY,
    drug_code varchar(50) NOT NULL,
    dur_type varchar(20) NOT NULL,
    source_version varchar(100) NOT NULL,
    verified boolean NOT NULL DEFAULT false,
    UNIQUE KEY uk_dur_coverage (drug_code, dur_type),
    FOREIGN KEY (drug_code) REFERENCES drug_master (drug_code)
);

ALTER TABLE dur_pregnancy
    MODIFY drug_name varchar(1000) NOT NULL,
    ADD source_record_key char(64) NULL UNIQUE,
    ADD runtime_verified boolean NOT NULL DEFAULT false,
    DROP INDEX uk_dur_pregnancy_product_code,
    ADD INDEX idx_dur_pregnancy_active (drug_code, runtime_verified),
    ADD FOREIGN KEY (source_record_key) REFERENCES dur_source_record (record_key);

ALTER TABLE dur_elderly
    MODIFY drug_name varchar(1000) NOT NULL,
    ADD source_record_key char(64) NULL UNIQUE,
    ADD runtime_verified boolean NOT NULL DEFAULT false,
    DROP INDEX uk_dur_elderly_category_product_code,
    ADD INDEX idx_dur_elderly_active (drug_code, runtime_verified),
    ADD FOREIGN KEY (source_record_key) REFERENCES dur_source_record (record_key);

ALTER TABLE dur_age
    MODIFY drug_name varchar(1000) NULL,
    MODIFY notice_number varchar(50) NULL,
    MODIFY notice_date date NULL,
    MODIFY warning_message text NULL,
    ADD drug_code varchar(50) NULL,
    ADD dur_product_code varchar(50) NULL,
    ADD ingredient_code varchar(50) NULL,
    ADD age_unit varchar(10) NOT NULL DEFAULT 'YEAR',
    ADD source_record_key char(64) NULL UNIQUE,
    ADD runtime_verified boolean NOT NULL DEFAULT false,
    ADD INDEX idx_dur_age_active (drug_code, runtime_verified),
    ADD FOREIGN KEY (drug_code) REFERENCES drug_master (drug_code),
    ADD FOREIGN KEY (source_record_key) REFERENCES dur_source_record (record_key);

ALTER TABLE dur_interaction
    MODIFY drug_name_a varchar(1000) NULL,
    MODIFY drug_name_b varchar(1000) NULL,
    MODIFY notice_number varchar(50) NULL,
    MODIFY notice_date date NULL,
    MODIFY warning_message text NULL,
    ADD drug_code_a varchar(50) NULL,
    ADD drug_code_b varchar(50) NULL,
    ADD dur_product_code_a varchar(50) NULL,
    ADD dur_product_code_b varchar(50) NULL,
    ADD ingredient_code_a varchar(50) NULL,
    ADD ingredient_code_b varchar(50) NULL,
    ADD source_record_key char(64) NULL UNIQUE,
    ADD runtime_verified boolean NOT NULL DEFAULT false,
    ADD INDEX idx_dur_interaction_active_a (drug_code_a, runtime_verified),
    ADD INDEX idx_dur_interaction_active_b (drug_code_b, runtime_verified),
    ADD FOREIGN KEY (drug_code_a) REFERENCES drug_master (drug_code),
    ADD FOREIGN KEY (drug_code_b) REFERENCES drug_master (drug_code),
    ADD FOREIGN KEY (source_record_key) REFERENCES dur_source_record (record_key);
