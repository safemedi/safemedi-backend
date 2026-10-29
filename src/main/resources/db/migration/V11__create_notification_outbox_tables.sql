ALTER TABLE `notification_log`
    ADD COLUMN `target_type` varchar(50) NULL COMMENT 'Navigation target type',
    ADD COLUMN `target_id` bigint NULL COMMENT 'Navigation target ID',
    ADD COLUMN `deduplication_key` varchar(255) NULL COMMENT 'Notification deduplication key';

ALTER TABLE `notification_log`
    ADD UNIQUE KEY `uk_notification_log_deduplication_key` (`deduplication_key`),
    ADD KEY `idx_notification_log_user_read_created` (`user_id`, `is_read`, `created_at`);

CREATE TABLE IF NOT EXISTS `notification_outbox` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'PK',
    `notification_log_id` bigint NOT NULL COMMENT 'Notification log ID',
    `user_id` bigint NOT NULL COMMENT 'Receiver user ID',
    `event_key` varchar(255) NOT NULL COMMENT 'Outbox deduplication key',
    `scheduled_at` datetime NOT NULL COMMENT 'Scheduled delivery time',
    `status` varchar(30) NOT NULL DEFAULT 'PENDING' COMMENT 'Outbox status',
    `retry_count` int NOT NULL DEFAULT 0 COMMENT 'Retry count',
    `next_retry_at` datetime NULL COMMENT 'Next retry time',
    `created_at` datetime COMMENT 'Created at',
    `updated_at` datetime COMMENT 'Updated at',
    UNIQUE KEY `uk_notification_outbox_event_key` (`event_key`),
    KEY `idx_notification_outbox_status_scheduled` (`status`, `scheduled_at`),
    CONSTRAINT `fk_notification_outbox_notification_log_id`
        FOREIGN KEY (`notification_log_id`) REFERENCES `notification_log` (`id`),
    CONSTRAINT `fk_notification_outbox_user_id`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) COMMENT 'Notification outbox';
