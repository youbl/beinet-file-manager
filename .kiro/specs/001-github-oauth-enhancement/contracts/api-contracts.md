# API 契约: GitHub OAuth 登录功能完善

## 认证相关接口

### POST /login/github
**目的**: GitHub OAuth 登录 (现有接口，保持兼容)

**请求**:
```json
{
  "code": "string"  // GitHub OAuth 授权码
}
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "张三",
    "email": "zhangsan@example.com",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "avatar": "https://avatars.githubusercontent.com/u/123456?v=4",
    "status": 1,
    "lastLoginDate": "2024-12-20T10:30:00",
    "loginCount": 5,
    "loginType": "GitHub",
    "githubId": 123456,
    "githubLogin": "zhangsan"
  }
}
```

**错误响应**:
```json
{
  "code": 401,
  "message": "GitHub 认证失败",
  "data": null
}
```

### POST /login/logout
**目的**: 用户登出 (新增接口)

**请求头**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**请求体**: 无

**响应**:
```json
{
  "code": 200,
  "message": "登出成功",
  "data": null
}
```

**错误响应**:
```json
{
  "code": 401,
  "message": "Token 无效或已过期",
  "data": null
}
```

### GET /login/user/info
**目的**: 获取当前登录用户信息 (新增接口)

**请求头**:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "张三",
    "email": "zhangsan@example.com",
    "avatar": "https://avatars.githubusercontent.com/u/123456?v=4",
    "status": 1,
    "lastLoginDate": "2024-12-20T10:30:00",
    "loginCount": 5,
    "loginType": "GitHub"
  }
}
```

## 用户管理接口

### GET /admin/users
**目的**: 获取用户列表 (扩展现有接口)

**请求参数**:
```
page: int = 1           // 页码
size: int = 20          // 每页大小
keyword: string = ""    // 搜索关键词 (用户名/邮箱)
status: int = null      // 用户状态筛选
loginType: int = null   // 登录类型筛选
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,
    "pages": 5,
    "current": 1,
    "size": 20,
    "records": [
      {
        "id": 1,
        "username": "张三",
        "email": "zhangsan@example.com",
        "avatar": "https://avatars.githubusercontent.com/u/123456?v=4",
        "status": 1,
        "loginType": "GitHub",
        "lastLoginDate": "2024-12-20T10:30:00",
        "loginCount": 5,
        "createTime": "2024-12-01T09:00:00",
        "githubLogin": "zhangsan"
      }
    ]
  }
}
```

### GET /admin/users/{id}
**目的**: 获取用户详细信息 (扩展现有接口)

**路径参数**:
- `id`: 用户ID

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "张三",
    "email": "zhangsan@example.com",
    "avatar": "https://avatars.githubusercontent.com/u/123456?v=4",
    "status": 1,
    "loginType": "GitHub",
    "lastLoginDate": "2024-12-20T10:30:00",
    "loginCount": 5,
    "createTime": "2024-12-01T09:00:00",
    "updateTime": "2024-12-20T10:30:00",
    "lastLoginIp": "192.168.1.100",
    "githubId": 123456,
    "githubLogin": "zhangsan",
    "githubAvatarUrl": "https://avatars.githubusercontent.com/u/123456?v=4",
    "githubNodeId": "MDQ6VXNlcjEyMzQ1Ng=="
  }
}
```

### PUT /admin/users/{id}/status
**目的**: 更新用户状态 (新增接口)

**路径参数**:
- `id`: 用户ID

**请求**:
```json
{
  "status": 0  // 0=禁用, 1=启用
}
```

**响应**:
```json
{
  "code": 200,
  "message": "用户状态更新成功",
  "data": null
}
```

## 审计日志接口

### GET /admin/audit-logs
**目的**: 获取审计日志列表 (新增接口)

**请求参数**:
```
page: int = 1               // 页码
size: int = 20              // 每页大小
userId: long = null         // 用户ID筛选
eventType: string = null    // 事件类型筛选
result: int = null          // 结果筛选 (0=失败, 1=成功)
startTime: string = null    // 开始时间 (ISO格式)
endTime: string = null      // 结束时间 (ISO格式)
ipAddress: string = null    // IP地址筛选
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 500,
    "pages": 25,
    "current": 1,
    "size": 20,
    "records": [
      {
        "id": 1,
        "userId": 1,
        "username": "张三",
        "eventType": "LOGIN",
        "eventDescription": "GitHub OAuth 登录成功",
        "ipAddress": "192.168.1.100",
        "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
        "requestUri": "/login/github",
        "result": 1,
        "errorMessage": null,
        "createTime": "2024-12-20T10:30:00"
      }
    ]
  }
}
```

### GET /admin/audit-logs/statistics
**目的**: 获取审计日志统计信息 (新增接口)

**请求参数**:
```
days: int = 7  // 统计天数，默认7天
```

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "totalEvents": 1000,
    "successEvents": 950,
    "failedEvents": 50,
    "successRate": 95.0,
    "eventTypeStats": [
      {
        "eventType": "LOGIN",
        "count": 800,
        "successCount": 780,
        "failedCount": 20
      },
      {
        "eventType": "LOGOUT",
        "count": 150,
        "successCount": 150,
        "failedCount": 0
      }
    ],
    "dailyStats": [
      {
        "date": "2024-12-20",
        "totalCount": 200,
        "successCount": 190,
        "failedCount": 10
      }
    ]
  }
}
```

## 系统配置接口

### GET /admin/config/whitelist
**目的**: 获取白名单配置 (新增接口)

**响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "enabled": true,
    "paths": [
      "/login/**",
      "/swagger-ui/**",
      "/v3/api-docs/**",
      "/static/**",
      "/favicon.ico",
      "/error",
      "/actuator/health"
    ]
  }
}
```

### PUT /admin/config/whitelist
**目的**: 更新白名单配置 (新增接口)

**请求**:
```json
{
  "enabled": true,
  "paths": [
    "/login/**",
    "/swagger-ui/**",
    "/v3/api-docs/**",
    "/static/**",
    "/favicon.ico",
    "/error",
    "/actuator/health",
    "/api/public/**"
  ]
}
```

**响应**:
```json
{
  "code": 200,
  "message": "白名单配置更新成功",
  "data": null
}
```

## 健康检查接口

### GET /actuator/health/auth
**目的**: 认证服务健康检查 (新增接口)

**响应**:
```json
{
  "status": "UP",
  "components": {
    "github": {
      "status": "UP",
      "details": {
        "lastCheck": "2024-12-20T10:30:00",
        "responseTime": "150ms"
      }
    },
    "database": {
      "status": "UP",
      "details": {
        "userCount": 100,
        "lastUserLogin": "2024-12-20T10:25:00"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "cachedUsers": 50,
        "cacheHitRate": "95%"
      }
    }
  }
}
```

## 错误码定义

### 认证相关错误码
```json
{
  "1001": "GitHub 授权码无效",
  "1002": "GitHub API 调用失败", 
  "1003": "JWT Token 无效",
  "1004": "JWT Token 已过期",
  "1005": "用户已被禁用",
  "1006": "登录频率过高，请稍后重试",
  "1007": "IP 地址被限制",
  "1008": "会话已过期，请重新登录"
}
```

### 权限相关错误码
```json
{
  "2001": "访问被拒绝，需要登录",
  "2002": "权限不足",
  "2003": "管理员权限不足",
  "2004": "资源不存在或无权访问"
}
```

### 系统相关错误码
```json
{
  "3001": "系统内部错误",
  "3002": "数据库连接失败",
  "3003": "Redis 连接失败",
  "3004": "GitHub 服务不可用",
  "3005": "配置错误"
}
```

## 请求/响应规范

### 通用请求头
```
Content-Type: application/json
Authorization: Bearer {token}  // 需要认证的接口
User-Agent: {client-info}      // 客户端信息
X-Request-ID: {uuid}           // 请求追踪ID (可选)
```

### 通用响应格式
```json
{
  "code": 200,           // 业务状态码
  "message": "success",  // 响应消息
  "data": {},           // 响应数据
  "timestamp": "2024-12-20T10:30:00Z",  // 响应时间
  "requestId": "uuid"   // 请求ID (如果提供)
}
```

### 分页响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "total": 100,      // 总记录数
    "pages": 5,        // 总页数
    "current": 1,      // 当前页码
    "size": 20,        // 每页大小
    "records": []      // 数据列表
  }
}
```

## 接口版本控制

### 版本策略
- 使用 URL 路径版本控制: `/api/v1/login/github`
- 当前版本: v1
- 向后兼容原则: 新版本不破坏现有客户端

### 版本映射
```
/login/github          -> /api/v1/login/github (兼容)
/login/logout          -> /api/v1/login/logout (新增)
/admin/users           -> /api/v1/admin/users (扩展)
/admin/audit-logs      -> /api/v1/admin/audit-logs (新增)
```