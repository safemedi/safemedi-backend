-- noinspection SqlResolveForFile

ALTER TABLE `access_token_blacklist`
    DROP INDEX `uk_access_token_blacklist_token`;

ALTER TABLE `access_token_blacklist`
    CHANGE COLUMN `token` `token_hash` varchar(512) NOT NULL COMMENT 'Discarded access token SHA-256 hash';

UPDATE `access_token_blacklist`
SET `token_hash` = SHA2(`token_hash`, 256)
WHERE CHAR_LENGTH(`token_hash`) <> 64
   OR `token_hash` NOT REGEXP '^[0-9a-fA-F]{64}$';

ALTER TABLE `access_token_blacklist`
    MODIFY COLUMN `token_hash` varchar(64) NOT NULL COMMENT 'Discarded access token SHA-256 hash';

ALTER TABLE `access_token_blacklist`
    ADD UNIQUE KEY `uk_access_token_blacklist_token_hash` (`token_hash`);
