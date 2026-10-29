CREATE TABLE IF NOT EXISTS `refresh_token` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'PK',
    `user_id` bigint NOT NULL COMMENT 'User ID (FK)',
    `token` varchar(512) NOT NULL COMMENT 'Refresh token',
    `created_at` datetime COMMENT 'Created at',
    `updated_at` datetime COMMENT 'Updated at',
    UNIQUE KEY `uk_refresh_token_user_id` (`user_id`),
    CONSTRAINT `fk_refresh_token_user_id`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) COMMENT 'Refresh token storage';
