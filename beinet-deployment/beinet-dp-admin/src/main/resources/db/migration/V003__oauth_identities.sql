-- Run after V001 and V002 on an existing database. Never auto-merge accounts by email.
CREATE TABLE oauth_identities (
    provider VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    subject VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (provider, subject),
    INDEX idx_oauth_user (user_id),
    CONSTRAINT fk_oauth_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB;

-- Preserve existing GitHub identities, including disabled/deleted users.
INSERT INTO oauth_identities(provider, subject, user_id)
SELECT 'github', CAST(github_id AS CHAR), id FROM users WHERE github_id IS NOT NULL;
