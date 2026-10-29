CREATE TABLE IF NOT EXISTS `access_token_blacklist` (
    `id` bigint PRIMARY KEY AUTO_INCREMENT COMMENT 'PK',
    `token` varchar(512) NOT NULL COMMENT 'Discarded access token',
    `expires_at` datetime NOT NULL COMMENT 'Access token expiration time',
    `created_at` datetime COMMENT 'Created at',
    `updated_at` datetime COMMENT 'Updated at',
    UNIQUE KEY `uk_access_token_blacklist_token` (`token`)
) COMMENT 'Access token blacklist';
