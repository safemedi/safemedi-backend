SET @refresh_tokens_exists := (
    SELECT COUNT(*)
    FROM information_schema.tables
    WHERE table_schema = DATABASE()
      AND table_name = 'refresh_tokens'
);

SET @copy_sql := IF(
    @refresh_tokens_exists > 0,
    'INSERT IGNORE INTO `refresh_token` (`user_id`, `token`) SELECT `user_id`, `token` FROM `refresh_tokens`',
    'SELECT 1'
);

PREPARE stmt FROM @copy_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @drop_sql := IF(
    @refresh_tokens_exists > 0,
    'DROP TABLE `refresh_tokens`',
    'SELECT 1'
);

PREPARE stmt FROM @drop_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
