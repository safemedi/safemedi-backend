ALTER TABLE `access_token_blacklist`
    ADD KEY `idx_access_token_blacklist_expires_at` (`expires_at`);
