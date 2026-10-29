ALTER TABLE `family_invitation`
    ADD COLUMN `accepted_at` datetime(6) NULL COMMENT 'UTC 기준 수락 시각',
    ADD COLUMN `accepted_by` bigint NULL COMMENT '초대를 수락한 사용자 ID',
    ADD KEY `idx_family_invitation_accepted_by` (`accepted_by`),
    ADD CONSTRAINT `fk_family_invitation_accepted_by`
        FOREIGN KEY (`accepted_by`) REFERENCES `user` (`id`);
