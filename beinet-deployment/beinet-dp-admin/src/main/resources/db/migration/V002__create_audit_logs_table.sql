-- 创建审计日志表
-- 执行时间: 2024-12-20

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
    INDEX idx_audit_time_result (create_time, result)
) COMMENT='审计日志表';

-- 创建事件类型检查约束
ALTER TABLE audit_logs ADD CONSTRAINT chk_event_type 
CHECK (event_type IN ('LOGIN', 'LOGOUT', 'AUTH_FAILED', 'TOKEN_EXPIRED', 'ACCESS_DENIED', 
                      'GITHUB_AUTH_START', 'GITHUB_AUTH_SUCCESS', 'GITHUB_AUTH_FAILED'));

-- 创建结果检查约束
ALTER TABLE audit_logs ADD CONSTRAINT chk_result 
CHECK (result IN (0, 1));