-- 扩展用户表以支持 GitHub OAuth 登录
-- 执行时间: 2024-12-20

-- 添加 GitHub 相关字段
ALTER TABLE users ADD COLUMN github_id BIGINT UNIQUE COMMENT 'GitHub用户ID';
ALTER TABLE users ADD COLUMN github_login VARCHAR(255) COMMENT 'GitHub用户名';
ALTER TABLE users ADD COLUMN github_avatar_url VARCHAR(500) COMMENT 'GitHub头像URL';
ALTER TABLE users ADD COLUMN github_node_id VARCHAR(255) COMMENT 'GitHub节点ID';
ALTER TABLE users ADD COLUMN login_type TINYINT DEFAULT 1 COMMENT '登录类型: 1=GitHub, 2=Google, 3=密码';
ALTER TABLE users ADD COLUMN last_login_ip VARCHAR(45) COMMENT '最后登录IP';
ALTER TABLE users ADD COLUMN login_count INT DEFAULT 0 COMMENT '登录次数';

-- 添加索引以提高查询性能
CREATE INDEX idx_users_github_id ON users(github_id);
CREATE INDEX idx_users_github_login ON users(github_login);
CREATE INDEX idx_users_login_type ON users(login_type);
CREATE INDEX idx_users_last_login ON users(lastLoginDate);
CREATE INDEX idx_users_status_type ON users(status, login_type);

-- 更新现有用户的登录类型 (如果有密码则为密码登录，否则为GitHub登录)
UPDATE users SET login_type = CASE 
    WHEN userPassword IS NOT NULL AND userPassword != '' THEN 3 
    ELSE 1 
END WHERE login_type IS NULL;

-- 初始化登录次数
UPDATE users SET login_count = 0 WHERE login_count IS NULL;