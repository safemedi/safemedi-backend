DROP TABLE IF EXISTS `family_request`;

CREATE TABLE IF NOT EXISTS `family_invitation` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT '기본 키',
    `inviter_id` bigint NOT NULL COMMENT '초대자 사용자 ID',
    `token_hash` varchar(64) NOT NULL COMMENT '초대 토큰의 SHA-256 해시',
    `encrypted_token` varchar(255) NOT NULL COMMENT 'AES-256-GCM으로 암호화한 초대 토큰',
    `status` varchar(20) NOT NULL DEFAULT 'PENDING' COMMENT '초대 상태',
    `expires_at` datetime(6) NOT NULL COMMENT 'UTC 기준 만료 시각',
    `created_at` datetime(6) NULL COMMENT 'UTC 기준 생성 시각',
    `updated_at` datetime(6) NULL COMMENT 'UTC 기준 수정 시각',
    UNIQUE KEY `uk_family_invitation_token_hash` (`token_hash`),
    KEY `idx_family_invitation_inviter_status_expires` (`inviter_id`, `status`, `expires_at`),
    KEY `idx_family_invitation_expires_at` (`expires_at`),
    CONSTRAINT `fk_family_invitation_inviter_id`
        FOREIGN KEY (`inviter_id`) REFERENCES `user` (`id`)
) COMMENT '일회용 가족 초대';
