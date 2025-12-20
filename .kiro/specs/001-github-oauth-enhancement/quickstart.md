# 快速开始: GitHub OAuth 登录功能完善

## 场景 1: GitHub OAuth 完整登录流程 (核心路径)

### 前置条件
- GitHub OAuth 应用已配置 (Client ID, Client Secret)
- 数据库表已创建并迁移
- 应用服务正常运行

### 测试步骤
1. **用户访问登录页面**
   ```bash
   curl -X GET http://localhost:8080/login
   # 预期: 返回登录页面或重定向到 GitHub OAuth
   ```

2. **用户点击 GitHub 登录**
   ```
   重定向到: https://github.com/login/oauth/authorize?client_id={CLIENT_ID}&redirect_uri={REDIRECT_URI}&scope=user:email
   ```

3. **用户在 GitHub 授权**
   ```
   用户同意授权后，GitHub 重定向回: 
   http://localhost:8080/login/github?code={AUTHORIZATION_CODE}
   ```

4. **系统处理 OAuth 回调**
   ```bash
   curl -X GET "http://localhost:8080/login/github?code=test_auth_code_123"
   ```

5. **预期结果**
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
       "loginType": "GitHub"
     }
   }
   ```

6. **验证 Cookie 设置**
   ```bash
   # 检查响应头中的 Set-Cookie
   # 应包含: Set-Cookie: LOGIN_TOKEN=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...; Path=/; HttpOnly; Secure
   ```

7. **验证数据库记录**
   ```sql
   -- 检查用户表
   SELECT * FROM users WHERE github_id = 123456;
   
   -- 检查审计日志
   SELECT * FROM audit_logs WHERE event_type = 'LOGIN' ORDER BY create_time DESC LIMIT 1;
   ```

### 成功标准
- ✅ 用户信息正确存储到数据库
- ✅ JWT Token 正确生成并设置到 Cookie
- ✅ 审计日志正确记录
- ✅ 整个流程在 30 秒内完成

---

## 场景 2: Token 验证和请求拦截

### 前置条件
- 用户已完成登录，获得有效 Token

### 测试步骤
1. **访问受保护的资源 (有效 Token)**
   ```bash
   curl -X GET http://localhost:8080/admin/users \
     -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
   ```

2. **预期结果**
   ```json
   {
     "code": 200,
     "message": "success",
     "data": {
       "total": 10,
       "records": [...]
     }
   }
   ```

3. **访问受保护的资源 (无 Token)**
   ```bash
   curl -X GET http://localhost:8080/admin/users
   ```

4. **预期结果**
   ```json
   {
     "code": 401,
     "message": "请重新登录: /admin/users",
     "data": null
   }
   ```

5. **访问白名单路径 (无 Token)**
   ```bash
   curl -X GET http://localhost:8080/swagger-ui/index.html
   ```

6. **预期结果**
   ```
   HTTP 200 OK
   # 正常返回 Swagger UI 页面
   ```

### 成功标准
- ✅ 有效 Token 可以访问受保护资源
- ✅ 无效/缺失 Token 被正确拦截
- ✅ 白名单路径可以无需认证访问
- ✅ Token 验证在 100ms 内完成

---

## 场景 3: 用户登出流程

### 前置条件
- 用户已登录，持有有效 Token

### 测试步骤
1. **执行登出操作**
   ```bash
   curl -X POST http://localhost:8080/login/logout \
     -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
   ```

2. **预期结果**
   ```json
   {
     "code": 200,
     "message": "登出成功",
     "data": null
   }
   ```

3. **验证 Token 失效**
   ```bash
   curl -X GET http://localhost:8080/admin/users \
     -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
   ```

4. **预期结果**
   ```json
   {
     "code": 401,
     "message": "Token 已失效，请重新登录",
     "data": null
   }
   ```

5. **验证审计日志**
   ```sql
   SELECT * FROM audit_logs WHERE event_type = 'LOGOUT' ORDER BY create_time DESC LIMIT 1;
   ```

### 成功标准
- ✅ 登出接口正确响应
- ✅ Token 被加入黑名单，后续请求被拒绝
- ✅ 登出事件被正确记录到审计日志

---

## 场景 4: 用户管理功能

### 前置条件
- 管理员用户已登录
- 系统中存在多个用户

### 测试步骤
1. **获取用户列表**
   ```bash
   curl -X GET "http://localhost:8080/admin/users?page=1&size=10" \
     -H "Authorization: Bearer {admin_token}"
   ```

2. **搜索用户**
   ```bash
   curl -X GET "http://localhost:8080/admin/users?keyword=张三" \
     -H "Authorization: Bearer {admin_token}"
   ```

3. **获取用户详情**
   ```bash
   curl -X GET "http://localhost:8080/admin/users/1" \
     -H "Authorization: Bearer {admin_token}"
   ```

4. **禁用用户**
   ```bash
   curl -X PUT "http://localhost:8080/admin/users/1/status" \
     -H "Authorization: Bearer {admin_token}" \
     -H "Content-Type: application/json" \
     -d '{"status": 0}'
   ```

5. **验证用户被禁用**
   ```bash
   # 被禁用用户尝试登录
   curl -X GET "http://localhost:8080/login/github?code=disabled_user_code"
   ```

6. **预期结果**
   ```json
   {
     "code": 1005,
     "message": "用户已被禁用",
     "data": null
   }
   ```

### 成功标准
- ✅ 用户列表正确分页显示
- ✅ 搜索功能正常工作
- ✅ 用户状态更新生效
- ✅ 被禁用用户无法登录

---

## 场景 5: 审计日志查询

### 前置条件
- 系统已运行一段时间，产生了审计日志

### 测试步骤
1. **获取审计日志列表**
   ```bash
   curl -X GET "http://localhost:8080/admin/audit-logs?page=1&size=20" \
     -H "Authorization: Bearer {admin_token}"
   ```

2. **按事件类型筛选**
   ```bash
   curl -X GET "http://localhost:8080/admin/audit-logs?eventType=LOGIN" \
     -H "Authorization: Bearer {admin_token}"
   ```

3. **按时间范围筛选**
   ```bash
   curl -X GET "http://localhost:8080/admin/audit-logs?startTime=2024-12-20T00:00:00&endTime=2024-12-20T23:59:59" \
     -H "Authorization: Bearer {admin_token}"
   ```

4. **获取统计信息**
   ```bash
   curl -X GET "http://localhost:8080/admin/audit-logs/statistics?days=7" \
     -H "Authorization: Bearer {admin_token}"
   ```

### 成功标准
- ✅ 审计日志正确记录和查询
- ✅ 筛选条件正常工作
- ✅ 统计信息准确计算

---

## 场景 6: 错误处理和边界情况

### 测试步骤
1. **GitHub 服务不可用**
   ```bash
   # 模拟 GitHub API 返回错误
   curl -X GET "http://localhost:8080/login/github?code=invalid_code"
   ```

2. **预期结果**
   ```json
   {
     "code": 1002,
     "message": "GitHub API 调用失败",
     "data": null
   }
   ```

3. **Token 过期**
   ```bash
   # 使用过期的 Token
   curl -X GET http://localhost:8080/admin/users \
     -H "Authorization: Bearer expired_token"
   ```

4. **预期结果**
   ```json
   {
     "code": 1004,
     "message": "JWT Token 已过期",
     "data": null
   }
   ```

5. **恶意请求 (SQL 注入尝试)**
   ```bash
   curl -X GET "http://localhost:8080/admin/users?keyword='; DROP TABLE users; --" \
     -H "Authorization: Bearer {admin_token}"
   ```

6. **预期结果**
   ```json
   {
     "code": 200,
     "message": "success",
     "data": {
       "total": 0,
       "records": []
     }
   }
   ```

### 成功标准
- ✅ 外部服务错误被正确处理
- ✅ Token 过期被正确识别
- ✅ 恶意输入被安全处理
- ✅ 所有错误都有相应的审计日志

---

## 场景 7: 性能验证

### 测试步骤
1. **Token 验证性能测试**
   ```bash
   # 使用 Apache Bench 进行并发测试
   ab -n 1000 -c 10 -H "Authorization: Bearer {valid_token}" \
     http://localhost:8080/login/user/info
   ```

2. **预期结果**
   ```
   Requests per second: > 1000 [#/sec]
   Time per request: < 100ms (mean)
   ```

3. **登录流程性能测试**
   ```bash
   # 测试完整登录流程的响应时间
   time curl -X GET "http://localhost:8080/login/github?code=test_code"
   ```

4. **预期结果**
   ```
   real    0m2.500s  # 应小于 30 秒
   ```

### 成功标准
- ✅ Token 验证 QPS > 1000
- ✅ 单次 Token 验证 < 100ms
- ✅ 完整登录流程 < 30 秒

---

## 场景 8: 数据一致性验证

### 测试步骤
1. **并发登录测试**
   ```bash
   # 同一用户多次并发登录
   for i in {1..5}; do
     curl -X GET "http://localhost:8080/login/github?code=same_user_code" &
   done
   wait
   ```

2. **验证数据一致性**
   ```sql
   -- 检查是否只创建了一个用户记录
   SELECT COUNT(*) FROM users WHERE github_id = 123456;
   -- 结果应该是 1
   
   -- 检查登录次数是否正确累加
   SELECT login_count FROM users WHERE github_id = 123456;
   -- 结果应该是 5
   ```

3. **缓存一致性测试**
   ```bash
   # 更新用户状态
   curl -X PUT "http://localhost:8080/admin/users/1/status" \
     -H "Authorization: Bearer {admin_token}" \
     -d '{"status": 0}'
   
   # 立即检查缓存是否更新
   curl -X GET "http://localhost:8080/login/user/info" \
     -H "Authorization: Bearer {user_token}"
   ```

### 成功标准
- ✅ 并发操作不产生重复数据
- ✅ 计数器正确累加
- ✅ 缓存与数据库保持一致

---

## 自动化测试脚本

### 完整流程测试脚本
```bash
#!/bin/bash

# 设置变量
BASE_URL="http://localhost:8080"
ADMIN_TOKEN=""
USER_TOKEN=""

echo "开始 GitHub OAuth 登录功能测试..."

# 场景 1: 登录流程
echo "测试场景 1: GitHub OAuth 登录"
response=$(curl -s -X GET "$BASE_URL/login/github?code=test_code")
echo "登录响应: $response"

# 提取 token
USER_TOKEN=$(echo $response | jq -r '.data.token')
echo "用户 Token: $USER_TOKEN"

# 场景 2: Token 验证
echo "测试场景 2: Token 验证"
curl -s -X GET "$BASE_URL/login/user/info" \
  -H "Authorization: Bearer $USER_TOKEN"

# 场景 3: 用户管理
echo "测试场景 3: 用户管理"
curl -s -X GET "$BASE_URL/admin/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# 场景 4: 登出
echo "测试场景 4: 用户登出"
curl -s -X POST "$BASE_URL/login/logout" \
  -H "Authorization: Bearer $USER_TOKEN"

echo "所有测试场景完成!"
```

### 性能测试脚本
```bash
#!/bin/bash

echo "开始性能测试..."

# Token 验证性能
echo "测试 Token 验证性能..."
ab -n 1000 -c 10 -H "Authorization: Bearer $USER_TOKEN" \
  "$BASE_URL/login/user/info"

# 登录流程性能
echo "测试登录流程性能..."
time curl -X GET "$BASE_URL/login/github?code=perf_test_code"

echo "性能测试完成!"
```