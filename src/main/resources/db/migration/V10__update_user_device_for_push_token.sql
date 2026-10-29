ALTER TABLE `user_device`
    MODIFY COLUMN `device_token` varchar(512) NOT NULL COMMENT 'FCM device token',
    MODIFY COLUMN `device_type` varchar(20) NOT NULL COMMENT 'Device type',
    ADD COLUMN `is_active` boolean NOT NULL DEFAULT true COMMENT 'Push token active status';

ALTER TABLE `user_device`
    ADD UNIQUE KEY `uk_user_device_device_token` (`device_token`);
