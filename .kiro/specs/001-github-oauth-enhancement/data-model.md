# 数据模型: GitHub OAuth 登录功能完善

## 实体: Users (扩展现有)

**目的**: 存储系统用户信息，支持 GitHub OAuth 登录

**字段扩展**:
```java
@Data
@Accessors(chain = true)
@TableName("users")
public class Users implements Serializable {
    
    // === 现有字段 (保持不变) ===
    @TableId(type = IdType.AUTO)
    private Long id;                    // 主键
    
    @Size(max = 255)
    @TableField(value = "`name`")
    private String name;                // 用户名称
    
    @Size(max = 255)
    @TableField(value = "`userEmail`")
    private String userEmail;           // 用户邮箱
    
    @TableField(value = "`lastLoginDate`")
    private LocalDateTime lastLoginDate; // 上次登录时间
    
    @TableField(value = "`status`")
    private Integer status;             // 状态，0：已禁用;1：已启用
    
    @Size(max = 255)
    @TableField(value = "`picture`")
    private String picture;             // 头像
    
    // ... 其他现有字段保持不变
    
    // === 新增字段 ===
    @TableField(value = "`githubId`")
    private Long githubId;              // GitHub用户ID (唯一)
    
    @Size(max = 255)
    @TableField(value = "`githubLogin`")
    private String githubLogin;         // GitHub用户名
    
    @Size(max = 500)
    @TableField(value = "`githubAvatarUrl`")
    private String githubAvatarUrl;     // GitHub头像URL
    
    @Size(max = 255)
    @TableField(value = "`githubNodeId`")
    private String githubNodeId;        // GitHub节点ID
    
    @TableField(value = "`loginType`")
    private Integer loginType;          // 登录类型: 1=GitHub, 2=Google, 3=密码
    
    @Size(max = 45)
    @TableField(value = "`lastLoginIp`")
    private String lastLoginIp;         // 最后登录IP
    
    @TableField(value = "`loginCount`")
    private Integer loginCount;         // 登录次数
}
```

**关系**: 
- 一对多关系到 AuditLogs (用户可以有多条审计日志)
- 通过 githubId 与 GitHub API 数据关联

**验证规则**:
- githubId 必须唯一 (数据库约束)
- userEmail 必须是有效邮箱格式
- status 只能是 0 或 1
- loginType 只能是 1, 2, 3
- loginCount 不能为负数

**状态转换**:
```
新用户注册: status=1, loginCount=0
首次登录: loginCount++, lastLoginDate=now(), lastLoginIp=clientIp
禁用用户: status=0 (无法登录)
启用用户: status=1 (可以登录)
```

## 实体: AuditLogs (新增)

**目的**: 记录用户登录和安全相关的操作日志，用于审计和监控

**字段**:
```java
@Data
@Accessors(chain = true)
@TableName("audit_logs")
public class AuditLogs implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;                    // 主键
    
    @TableField(value = "`userId`")
    private Long userId;                // 用户ID，关联users表
    
    @Size(max = 50)
    @TableField(value = "`eventType`")
    private String eventType;           // 事件类型
    
    @Size(max = 500)
    @TableField(value = "`eventDescription`")
    private String eventDescription;    // 事件描述
    
    @Size(max = 45)
    @TableField(value = "`ipAddress`")
    private String ipAddress;           // 客户端IP地址
    
    @Size(max = 1000)
    @TableField(value = "`userAgent`")
    private String userAgent;           // 用户代理字符串
    
    @Size(max = 500)
    @TableField(value = "`requestUri`")
    private String requestUri;          // 请求URI
    
    @Size(max = 100)
    @TableField(value = "`sessionId`")
    private String sessionId;           // 会话ID
    
    @TableField(value = "`result`")
    private Integer result;             // 结果: 0=失败, 1=成功
    
    @Size(max = 500)
    @TableField(value = "`errorMessage`")
    private String errorMessage;        // 错误信息（如果有）
    
    @TableField(value = "`createTime`", insertStrategy = FieldStrategy.NEVER)
    private LocalDateTime createTime;   // 创建时间
}
```

**关系**:
- 多对一关系到 Users (多条日志属于一个用户)
- userId 外键关联到 users.id

**验证规则**:
- eventType 必须是预定义的枚举值
- result 只能是 0 或 1
- ipAddress 必须是有效的 IP 地址格式
- createTime 自动设置为当前时间

**事件类型枚举**:
```java
public enum AuditEventType {
    LOGIN("LOGIN", "用户登录"),
    LOGOUT("LOGOUT", "用户登出"),
    AUTH_FAILED("AUTH_FAILED", "认证失败"),
    TOKEN_EXPIRED("TOKEN_EXPIRED", "Token过期"),
    ACCESS_DENIED("ACCESS_DENIED", "访问被拒绝"),
    GITHUB_AUTH_START("GITHUB_AUTH_START", "GitHub认证开始"),
    GITHUB_AUTH_SUCCESS("GITHUB_AUTH_SUCCESS", "GitHub认证成功"),
    GITHUB_AUTH_FAILED("GITHUB_AUTH_FAILED", "GitHub认证失败");
    
    private final String code;
    private final String description;
}
```

## 实体: WhitelistConfig (配置类)

**目的**: 管理登录白名单配置

**字段**:
```java
@Data
@ConfigurationProperties(prefix = "auth.whitelist")
@Component
public class WhitelistConfig {
    
    private List<String> paths = Arrays.asList(
        "/login/**",
        "/swagger-ui/**", 
        "/v3/api-docs/**",
        "/static/**",
        "/favicon.ico",
        "/error",
        "/actuator/health"
    );
    
    private boolean enabled = true;     // 是否启用白名单检查
}
```

**验证规则**:
- paths 不能为空
- 路径必须以 / 开头
- 支持 Ant 风格的路径匹配模式

## DTO 映射关系

### UserDto (扩展现有)

**目的**: 用于 API 响应的用户信息传输

```java
@Data
@Accessors(chain = true)
public class UserDto {
    private Long id;
    private String username;            // 对应 Users.name
    private String email;               // 对应 Users.userEmail
    private String token;               // JWT Token (登录时返回)
    private String avatar;              // 对应 Users.picture 或 githubAvatarUrl
    private Integer status;             // 对应 Users.status
    private LocalDateTime lastLoginDate; // 对应 Users.lastLoginDate
    private Integer loginCount;         // 对应 Users.loginCount
    private String loginType;           // 登录类型描述
    
    // GitHub 相关信息 (可选返回)
    private Long githubId;              // 对应 Users.githubId
    private String githubLogin;         // 对应 Users.githubLogin
}
```

### AuditLogDto

**目的**: 用于审计日志的传输和查询

```java
@Data
@Accessors(chain = true)
public class AuditLogDto {
    private Long id;
    private Long userId;
    private String username;            // 关联查询 Users.name
    private String eventType;
    private String eventDescription;
    private String ipAddress;
    private String userAgent;
    private String requestUri;
    private Integer result;
    private String errorMessage;
    private LocalDateTime createTime;
}
```

### LoginRequestDto

**目的**: 登录请求参数

```java
@Data
public class LoginRequestDto {
    @NotBlank(message = "授权码不能为空")
    private String code;                // GitHub OAuth 授权码
    
    private String state;               // 可选的状态参数
    private String redirectUri;         // 重定向URI
}
```

## 数据库索引策略

### Users 表索引
```sql
-- 主键索引 (自动创建)
PRIMARY KEY (id)

-- GitHub 相关索引
CREATE UNIQUE INDEX idx_users_github_id ON users(github_id);
CREATE INDEX idx_users_github_login ON users(github_login);

-- 查询优化索引
CREATE INDEX idx_users_email ON users(userEmail);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_users_login_type ON users(loginType);
CREATE INDEX idx_users_last_login ON users(lastLoginDate);

-- 复合索引
CREATE INDEX idx_users_status_type ON users(status, loginType);
```

### AuditLogs 表索引
```sql
-- 主键索引 (自动创建)
PRIMARY KEY (id)

-- 外键索引
CREATE INDEX idx_audit_user_id ON audit_logs(userId);

-- 查询优化索引
CREATE INDEX idx_audit_event_type ON audit_logs(eventType);
CREATE INDEX idx_audit_create_time ON audit_logs(createTime);
CREATE INDEX idx_audit_ip_address ON audit_logs(ipAddress);
CREATE INDEX idx_audit_result ON audit_logs(result);

-- 复合索引 (常用查询组合)
CREATE INDEX idx_audit_user_event ON audit_logs(userId, eventType);
CREATE INDEX idx_audit_time_result ON audit_logs(createTime, result);
```

## 缓存策略

### Redis 缓存键设计
```java
// 用户信息缓存
"user:github:{githubId}" -> UserDto (TTL: 1小时)
"user:id:{userId}" -> UserDto (TTL: 1小时)

// 白名单配置缓存
"auth:whitelist:paths" -> List<String> (TTL: 24小时)

// Token 黑名单 (登出后的 Token)
"auth:blacklist:{tokenHash}" -> "1" (TTL: Token剩余有效期)

// 登录限流缓存
"auth:limit:ip:{ip}" -> 登录次数 (TTL: 1小时)
"auth:limit:user:{userId}" -> 登录次数 (TTL: 1小时)
```

### 缓存更新策略
- **用户信息**: 登录时写入，用户信息更新时失效
- **白名单配置**: 应用启动时加载，配置变更时刷新
- **Token 黑名单**: 用户登出时添加，Token 过期时自动清理
- **限流计数**: 滑动窗口计数，超过阈值时拒绝请求