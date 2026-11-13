-- 全新安装：直接创建最终数据库结构，仅执行一次。
-- MySQL 8.x（最低 8.0.16）。使用 DBA 账户执行，不使用 --force。
-- 数据库已存在时停止；本脚本不删除或覆盖已有数据。
CREATE DATABASE beinet_file_manager CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE beinet_file_manager;

CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY COMMENT '本地用户ID，自增主键',
    name VARCHAR(255) COMMENT '用户名称',
    userPassword VARCHAR(255) COMMENT '密码字段，当前OAuth登录不使用',
    userEmail VARCHAR(255) COMMENT '用户邮箱',
    lastLoginDate DATETIME COMMENT '最近一次登录时间',
    status INT NOT NULL DEFAULT 1 COMMENT '账号状态：0=禁用，1=启用',
    isBoss INT NOT NULL DEFAULT 0 COMMENT 'Boss标记：0=否，1=是',
    isAdmin INT NOT NULL DEFAULT 0 COMMENT '管理员标记：0=否，1=是；当前权限以YML管理员邮箱列表为准',
    tenantId BIGINT COMMENT '租户ID',
    createTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    updateTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '记录最后更新时间',
    delflag INT NOT NULL DEFAULT 0 COMMENT '软删除标记：0=未删除，1=已删除',
    inviteUserId BIGINT COMMENT '邀请人用户ID',
    inviteToken VARCHAR(64) COMMENT '邀请令牌',
    mfaKey VARCHAR(100) COMMENT 'MFA密钥，当前OAuth登录不使用',
    picture VARCHAR(255) COMMENT '用户头像地址',
    isTest INT DEFAULT 0 COMMENT '测试人员标记：0=否，1=是',
    memo VARCHAR(10) COMMENT '用户备注',
    userIp VARCHAR(15) COMMENT '用户注册IP地址',
    userLoc VARCHAR(50) COMMENT '用户注册IP归属地',
    github_id BIGINT COMMENT 'GitHub用户ID',
    github_login VARCHAR(255) COMMENT 'GitHub用户名',
    github_avatar_url VARCHAR(500) COMMENT 'GitHub头像URL',
    github_node_id VARCHAR(255) COMMENT 'GitHub节点ID',
    login_type TINYINT DEFAULT 1 COMMENT '登录类型: 1=GitHub, 2=Google, 3=密码',
    last_login_ip VARCHAR(45) COMMENT '最后登录IP',
    login_count INT DEFAULT 0 COMMENT '登录次数',
    UNIQUE KEY uk_users_github_id (github_id),
    INDEX idx_users_github_login (github_login),
    INDEX idx_users_login_type (login_type),
    INDEX idx_users_last_login (lastLoginDate),
    INDEX idx_users_status_type (status, login_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户账号表';

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT COMMENT '用户ID，关联users表',
    event_type VARCHAR(50) NOT NULL COMMENT '事件类型: LOGIN, LOGOUT, AUTH_FAILED, TOKEN_EXPIRED',
    event_description VARCHAR(500) COMMENT '事件描述',
    ip_address VARCHAR(45) COMMENT '客户端IP地址',
    user_agent VARCHAR(1000) COMMENT '用户代理字符串',
    request_uri VARCHAR(500) COMMENT '请求URI',
    session_id VARCHAR(100) COMMENT '会话ID',
    result TINYINT NOT NULL COMMENT '结果: 0=失败, 1=成功',
    error_message VARCHAR(500) COMMENT '错误信息（如果有）',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    -- 外键约束
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,

    -- 索引
    INDEX idx_audit_user_id (user_id),
    INDEX idx_audit_event_type (event_type),
    INDEX idx_audit_create_time (create_time),
    INDEX idx_audit_ip_address (ip_address),
    INDEX idx_audit_result (result),
    INDEX idx_audit_user_event (user_id, event_type),
    INDEX idx_audit_time_result (create_time, result),
    CONSTRAINT chk_event_type CHECK (
        event_type IN ('LOGIN', 'LOGOUT', 'AUTH_FAILED', 'TOKEN_EXPIRED',
                       'ACCESS_DENIED', 'GITHUB_AUTH_START',
                       'GITHUB_AUTH_SUCCESS', 'GITHUB_AUTH_FAILED')
    ),
    CONSTRAINT chk_result CHECK (result IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

CREATE TABLE oauth_identities (
    provider VARCHAR(20) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '身份供应商：google或github',
    subject VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '供应商内唯一用户标识：Google sub或GitHub用户ID',
    user_id BIGINT NOT NULL COMMENT '关联的本地用户ID，对应users.id',
    PRIMARY KEY (provider, subject),
    INDEX idx_oauth_user (user_id),
    CONSTRAINT fk_oauth_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='OAuth供应商身份与本地用户关联表';
