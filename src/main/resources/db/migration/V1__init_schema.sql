-- =====================================
-- 1. 마스터 데이터 (의약품 및 질환 지식 베이스)
-- =====================================

CREATE TABLE `atc_group_master` (
    `atc_code`    varchar(20)  PRIMARY KEY COMMENT 'ATC 분류 코드 (PK)',
    `atc_name_ko` varchar(255) COMMENT 'ATC 한글 분류명',
    `atc_name_en` varchar(255) COMMENT 'ATC 영문 분류명',
    `atc_level`   int          COMMENT 'ATC 분류 계층 단계 (1~5단계)'
) COMMENT 'ATC 분류 마스터';

CREATE TABLE `ingredient_master` (
    `ingredient_code` varchar(50)  PRIMARY KEY COMMENT '약품 성분 고유 코드 (PK)',
    `ingredient_name` varchar(255) COMMENT '약품 성분명'
) COMMENT '약품 성분 마스터';

CREATE TABLE `drug_master` (
    `drug_code` varchar(50)  PRIMARY KEY COMMENT '의약품 제품 고유 코드 (PK)',
    `drug_name` varchar(255) COMMENT '의약품 제품명',
    `atc_code`  varchar(20)  COMMENT '해당 의약품의 대표 ATC 코드'
) COMMENT '의약품(제품) 마스터';

CREATE TABLE `ingredient_atc_map` (
    `id`              bigint      PRIMARY KEY AUTO_INCREMENT COMMENT 'PK',
    `ingredient_code` varchar(50) COMMENT '매핑될 약품 성분 코드 (FK)',
    `atc_code`        varchar(20) COMMENT '매핑될 ATC 코드 (FK)'
) COMMENT '성분-ATC 다대다 매핑';

CREATE TABLE `drug_ingredient_map` (
    `id`              bigint      PRIMARY KEY AUTO_INCREMENT COMMENT 'PK',
    `drug_code`       varchar(50) COMMENT '매핑될 의약품 코드 (FK)',
    `ingredient_code` varchar(50) COMMENT '매핑될 약품 성분 코드 (FK)'
) COMMENT '의약품-성분 다대다 매핑';

CREATE TABLE `disease_master` (
    `disease_code` varchar(50)  PRIMARY KEY COMMENT '기저질환 고유 식별 코드 (PK)',
    `disease_name` varchar(255) COMMENT '표준화된 기저질환명'
) COMMENT '기저질환 마스터';

-- =====================================
-- 2. 사용자 계정 및 프로필
-- =====================================

CREATE TABLE `user` (
    `id`                    bigint      PRIMARY KEY AUTO_INCREMENT COMMENT '사용자 고유 식별자 (PK)',
    `nickname`              varchar(50) COMMENT '사용자 닉네임',
    `social_provider`       ENUM('GOOGLE') COMMENT '가입에 사용된 소셜 플랫폼',
    `social_id`             varchar(255) UNIQUE COMMENT '소셜 플랫폼 고유 식별값',
    `invite_code`           varchar(20)  COMMENT '가족 초대용 고유 난수 코드',
    `is_tutorial_completed` boolean      DEFAULT false COMMENT '온보딩 완료 여부',
    `created_at`            datetime     COMMENT '계정 최초 생성 일시',
    `updated_at`            datetime     COMMENT '계정 정보 최근 수정 일시'
) COMMENT '사용자 기본 계정 정보';

CREATE TABLE `user_health_profile` (
    `user_id`    bigint PRIMARY KEY COMMENT '사용자 식별자 (PK 겸 FK)',
    `birth_date` date   COMMENT '사용자의 생년월일',
    `gender`     ENUM('MALE', 'FEMALE') COMMENT '사용자의 성별',
    `height`     int    COMMENT '사용자의 키 (cm)',
    `weight`     int    COMMENT '사용자의 몸무게 (kg)',
    `blood_type` ENUM('A', 'B', 'O', 'AB') COMMENT 'ABO식 혈액형',
    `rh_type`    ENUM('PLUS', 'MINUS') DEFAULT 'PLUS' COMMENT 'Rh식 혈액형',
    `created_at` datetime,
    `updated_at` datetime COMMENT '건강 프로필 최근 수정 일시'
) COMMENT '사용자 신체/건강 프로필';

CREATE TABLE `user_disease_map` (
    `id`           bigint      PRIMARY KEY AUTO_INCREMENT COMMENT 'PK',
    `user_id`      bigint      COMMENT '사용자 식별자 (FK)',
    `disease_code` varchar(50) COMMENT '기저질환 코드 (FK)',
    `created_at`   datetime    COMMENT '기저질환 최초 등록 일시'
) COMMENT '사용자-기저질환 매핑';

CREATE TABLE `user_device` (
    `id`                    bigint       PRIMARY KEY AUTO_INCREMENT COMMENT 'PK',
    `user_id`               bigint       COMMENT '사용자 식별자 (FK)',
    `device_token`          varchar(255) COMMENT '푸시 알림용 기기 토큰',
    `device_type`           varchar(20)  COMMENT '기기 OS 종류 (iOS, Android)',
    `is_my_reminder_on`     boolean      DEFAULT true COMMENT '본인 알림 수신 동의',
    `is_family_reminder_on` boolean      DEFAULT true COMMENT '가족 알림 수신 동의',
    `is_missed_alert_on`    boolean      DEFAULT true COMMENT '미복약 알림 수신 동의',
    `created_at`            datetime     COMMENT '디바이스 최초 등록 일시',
    `updated_at`            datetime     COMMENT '디바이스 정보 최근 수정 일시'
) COMMENT '사용자 기기 및 알림 설정';

CREATE TABLE `user_allergy` (
    `id`                  bigint       PRIMARY KEY AUTO_INCREMENT COMMENT 'PK',
    `user_id`             bigint       COMMENT '사용자 식별자 (FK)',
    `allergy_type`        ENUM('ATC_GROUP', 'INGREDIENT', 'FOOD') COMMENT '알러지 분류',
    `allergy_value`       varchar(50)  COMMENT '알러지 원인값',
    `allergy_name`        varchar(255) COMMENT '화면 표시용 알러지 이름',
    `registered_via_drug` varchar(50)  COMMENT '등록 원인 약물 코드 (FK)',
    `created_at`            datetime,
    `updated_at`            datetime
) COMMENT '사용자 알러지 정보';

-- =====================================
-- 3. 가족 관계
-- =====================================

CREATE TABLE `family` (
    `id`                bigint      PRIMARY KEY AUTO_INCREMENT COMMENT 'PK',
    `user_id`           bigint      COMMENT '주체 사용자 (FK)',
    `connected_user_id` bigint      COMMENT '가족 대상 사용자 (FK)',
    `relation`          varchar(50) COMMENT '가족 관계 호칭',
    `is_allow_my_info`  boolean     DEFAULT true COMMENT '상대방에게 내 정보 노출 허용 여부',
    `is_receive_alert`  boolean     DEFAULT true COMMENT '상대방의 알림 수신 여부',
    `created_at`        datetime    COMMENT '관계 성립 일시',
    `updated_at`        datetime    COMMENT '최근 수정 일시'
) COMMENT '가족 연결 정보';

CREATE TABLE `family_request` (
    `id`                    bigint      PRIMARY KEY AUTO_INCREMENT COMMENT 'PK',
    `sender_id`             bigint      COMMENT '요청자 (FK)',
    `receiver_id`           bigint      COMMENT '수신자 (FK)',
    `proposed_relation`     varchar(50) COMMENT '제안 호칭',
    `status`                ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELLED', 'EXPIRED') DEFAULT 'PENDING' COMMENT '요청 상태',
    `sender_allow_my_info`  boolean     DEFAULT true COMMENT '보낸 사람의 내 정보 공유 허용 여부',
    `sender_receive_alert`  boolean     DEFAULT true COMMENT '보낸 사람의 상대방 알림 수신 여부',
    `created_at`            datetime    COMMENT '요청 발송 일시',
    `updated_at`            datetime    COMMENT '상태 변경 일시'
) COMMENT '가족 연결 요청 내역';

-- =====================================
-- 4. 처방전 및 복약 관리
-- =====================================

CREATE TABLE `prescription` (
    `id`                   bigint       PRIMARY KEY AUTO_INCREMENT COMMENT 'PK',
    `user_id`              bigint       COMMENT '소유자 (FK)',
    `title`                varchar(255) COMMENT '처방전 별칭',
    `has_allergy_conflict` boolean      DEFAULT false COMMENT '알러지/질환 충돌 경고 발생 여부',
    `is_doctor_approved`   boolean      DEFAULT false COMMENT '경고 발생 시 의사 승인 체크 여부',
    `start_date`           date         COMMENT '복용 시작일',
    `end_date`             date         COMMENT '복용 종료일',
    `created_at`           datetime     COMMENT '등록 일시',
    `updated_at`           datetime     COMMENT '최근 수정 일시'
) COMMENT '처방전 기본 정보';

CREATE TABLE `prescription_drug` (
    `id`              bigint       PRIMARY KEY AUTO_INCREMENT COMMENT 'PK',
    `prescription_id` bigint       COMMENT '처방전 식별자 (FK)',
    `drug_name`       varchar(255) COMMENT '약품명 (스냅샷)',
    `drug_code`       varchar(50)  COMMENT '의약품 코드 (FK)',
    `atc_code`        varchar(20)  COMMENT '당시 대표 ATC 코드 (스냅샷)'
) COMMENT '처방전 포함 약품';

CREATE TABLE `prescription_drug_time` (
    `id`                   bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'PK',
    `prescription_drug_id` bigint COMMENT '처방 약품 식별자 (FK)',
    `take_time`            time   COMMENT '복용 시간 (HH:mm)'
) COMMENT '약품별 복용 시간';

CREATE TABLE `medication_record` (
    `id`              bigint   PRIMARY KEY AUTO_INCREMENT COMMENT 'PK',
    `user_id`         bigint   COMMENT '사용자 (FK)',
    `prescription_id` bigint   COMMENT '처방전 (FK)',
    `scheduled_at`    datetime COMMENT '예정 일시',
    `taken_at`        datetime COMMENT '실제 복용 일시',
    `status`          ENUM('PENDING', 'SUCCESS', 'FAIL', 'SKIP') DEFAULT 'PENDING' COMMENT '복약 상태',
    `created_at`            datetime,
    `updated_at`            datetime
) COMMENT '복약 수행 기록';

CREATE TABLE `notification_log` (
    `id`         bigint       PRIMARY KEY AUTO_INCREMENT COMMENT 'PK',
    `user_id`    bigint       COMMENT '수신자 (FK)',
    `type`       varchar(50)  COMMENT '알림 유형',
    `title`      varchar(255) COMMENT '알림 제목',
    `message`    text         COMMENT '알림 내용',
    `is_read`    boolean      DEFAULT false COMMENT '읽음 여부',
    `created_at` datetime     COMMENT '발송 일시',
    `updated_at` datetime     COMMENT '상태 변경 일시'
) COMMENT '알림 발송 이력';

-- =====================================
-- 5. DUR (의약품안전사용서비스) 금기 및 주의 마스터
-- =====================================

CREATE TABLE `dur_interaction` (
    `id`              bigint PRIMARY KEY AUTO_INCREMENT COMMENT '병용 금기 식별자 (PK)',
    `drug_name_a`     varchar(255) COMMENT '기준 약품명',
    `drug_name_b`     varchar(255) COMMENT '대상 약품명',
    `notice_number`   varchar(50)  COMMENT '심평원 고시번호',
    `notice_date`     date         COMMENT '심평원 고시일자',
    `warning_message` text         COMMENT '부작용 및 경고 상세 정보'
) COMMENT 'DUR 병용 금기';

CREATE TABLE `dur_age` (
    `id`              bigint PRIMARY KEY AUTO_INCREMENT COMMENT '연령 금기 식별자 (PK)',
    `drug_name`       varchar(255) COMMENT '제한 대상 약품명',
    `target_age`      int          COMMENT '제한 기준 연령',
    `age_condition`   varchar(20)  COMMENT '연령 조건 (UNDER, OVER 등)',
    `notice_number`   varchar(50)  COMMENT '심평원 고시번호',
    `notice_date`     date         COMMENT '심평원 고시일자',
    `warning_message` text         COMMENT '부작용 상세 정보'
) COMMENT 'DUR 연령 금기';

CREATE TABLE `dur_pregnancy` (
    `id`              bigint PRIMARY KEY AUTO_INCREMENT COMMENT '임부 금기 식별자 (PK)',
    `drug_name`       varchar(255) COMMENT '임부 투여 제한 약품명',
    `grade`           int          COMMENT '금기 등급',
    `notice_number`   varchar(50)  COMMENT '심평원 고시번호',
    `notice_date`     date         COMMENT '심평원 고시일자',
    `warning_message` text         COMMENT '경고 상세 정보'
) COMMENT 'DUR 임부 금기';

CREATE TABLE `dur_elderly` (
    `id`              bigint PRIMARY KEY AUTO_INCREMENT COMMENT '노인 주의 식별자 (PK)',
    `drug_name`       varchar(255) COMMENT '고령자 투여 주의 약품명',
    `notice_number`   varchar(50)  COMMENT '심평원 공고번호',
    `notice_date`     date         COMMENT '심평원 공고일자',
    `warning_message` text         COMMENT '경고 상세 정보'
) COMMENT 'DUR 노인 주의';

-- =====================================
-- 6. 관계 설정 (Foreign Keys)
-- =====================================

ALTER TABLE `ingredient_atc_map`  ADD FOREIGN KEY (`ingredient_code`)   REFERENCES `ingredient_master` (`ingredient_code`);
ALTER TABLE `drug_ingredient_map` ADD FOREIGN KEY (`drug_code`)         REFERENCES `drug_master` (`drug_code`);
ALTER TABLE `drug_ingredient_map` ADD FOREIGN KEY (`ingredient_code`)   REFERENCES `ingredient_master` (`ingredient_code`);

ALTER TABLE `user_health_profile` ADD FOREIGN KEY (`user_id`)             REFERENCES `user` (`id`);
ALTER TABLE `user_disease_map`    ADD FOREIGN KEY (`user_id`)             REFERENCES `user` (`id`);
ALTER TABLE `user_disease_map`    ADD FOREIGN KEY (`disease_code`)        REFERENCES `disease_master` (`disease_code`);
ALTER TABLE `user_device`         ADD FOREIGN KEY (`user_id`)             REFERENCES `user` (`id`);
ALTER TABLE `user_allergy`        ADD FOREIGN KEY (`user_id`)             REFERENCES `user` (`id`);
ALTER TABLE `user_allergy`        ADD FOREIGN KEY (`registered_via_drug`) REFERENCES `drug_master` (`drug_code`);

ALTER TABLE `family`              ADD FOREIGN KEY (`user_id`)             REFERENCES `user` (`id`);
ALTER TABLE `family`              ADD FOREIGN KEY (`connected_user_id`)   REFERENCES `user` (`id`);
ALTER TABLE `family_request`      ADD FOREIGN KEY (`sender_id`)           REFERENCES `user` (`id`);
ALTER TABLE `family_request`      ADD FOREIGN KEY (`receiver_id`)         REFERENCES `user` (`id`);

ALTER TABLE `prescription`          ADD FOREIGN KEY (`user_id`)              REFERENCES `user` (`id`);
ALTER TABLE `prescription_drug`     ADD FOREIGN KEY (`prescription_id`)      REFERENCES `prescription` (`id`);
ALTER TABLE `prescription_drug`     ADD FOREIGN KEY (`drug_code`)            REFERENCES `drug_master` (`drug_code`);
ALTER TABLE `prescription_drug_time` ADD FOREIGN KEY (`prescription_drug_id`) REFERENCES `prescription_drug` (`id`);

ALTER TABLE `medication_record`   ADD FOREIGN KEY (`user_id`)             REFERENCES `user` (`id`);
ALTER TABLE `medication_record`   ADD FOREIGN KEY (`prescription_id`)     REFERENCES `prescription` (`id`);
ALTER TABLE `notification_log`    ADD FOREIGN KEY (`user_id`)             REFERENCES `user` (`id`);
