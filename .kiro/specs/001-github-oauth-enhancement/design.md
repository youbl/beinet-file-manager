# 技术实现计划: GitHub OAuth 登录功能完善

**功能分支**: `001-github-oauth-enhancement` | **日期**: 2024年12月20日 | **规格**: [requirements.md](./requirements.md)

## 概述

基于现有的 Spring Boot 3.4.5 架构，完善 GitHub OAuth 登录功能，包括用户信息持久化、登录状态管理、请求拦截优化和审计日志。利用现有的技术栈，无需引入新的外部依赖。

## 技术上下文

**语言/版本**: Java 21  
**主要依赖**: Spring Boot 3.4.5, MyBatis Plus 3.5.9, JWT 0.12.6, Spring Cloud OpenFeign  
**存储**: MySQL (用户数据) + Redis (缓存) + 数据库 (审计日志)  
**测试**: JUnit 5 (现有)  
**目标平台**: Linux 服务器  
**项目类型**: 微服务架构  
**性能目标**: 1000 TPS 认证验证，100ms Token 验证响应  
**约束**: 必须与现有 Spring Boot 架构兼容，支持水平扩展  
**规模/范围**: 企业级文件管理系统的认证模块  

## 治理检查

*门禁: 必须在第0阶段研究前通过。第1阶段设计后重新检查。*

### 产品对齐
- [x] 符合产品愿景？- 提供安全便捷的 GitHub OAuth 登录
- [x] 遵守业务约束？- 符合企业安全要求
- [x] 满足非功能需求？- 满足性能和安全要求

### 技术合规
- [x] 使用批准的技术栈？- 基于现有 Spring Boot 架构
- [x] 遵循开发原则？- 遵循微服务和分层架构原则
- [x] 通过质量门禁？- 包含完整的测试策略

### 结构合规
- [x] 遵循项目布局？- 符合现有模块化结构
- [x] 使用正确的命名约定？- 遵循现有命名规范
- [x] 遵守模块组织规则？- 符合业务模块和核心模块分离

## 项目结构

### 文档 (此功能)
```text
.kiro/specs/001-github-oauth-enhancement/
├── design.md            # 本文件
├── research.md          # 第0阶段输出
├── data-model.md        # 第1阶段输出
├── quickstart.md        # 第1阶段输出
├── contracts/           # 第1阶段输出
└── tasks.md             # 任务工作流输出
```

### 源代码
```text
beinet-business/beinet-login/
├── src/main/java/cn/beinet/business/login/
│   ├── LoginController.java          # 现有，需扩展
│   ├── service/
│   │   ├── LoginService.java         # 现有，需扩展
│   │   └── UserManagementService.java # 新增
│   ├── loginValidate/
│   │   ├── Validator.java            # 现有
│   │   ├── TokenValidator.java       # 现有
│   │   ├── CookieValidator.java      # 现有
│   │   └── NoNeedLoginValidator.java # 现有，需扩展
│   └── dal/
│       ├── entity/
│       │   └── Users.java            # 扩展现有
│       └── mapper/
│           └── UsersMapper.java      # 新增

beinet-deployment/beinet-dp-admin/
├── src/main/java/cn/beinet/deployment/admin/
│   ├── autoConfig/
│   │   └── AuthorizationFilter.java  # 现有，需优化
│   └── users/
│       ├── controller/
│       │   └── UsersController.java  # 现有，需扩展
│       └── service/
│           └── UsersService.java     # 现有，需扩展

beinet-sdk/beinet-login-sdk/
├── src/main/java/cn/beinet/sdk/login/
│   ├── LoginSdk.java                 # 现有，需扩展
│   └── dto/
│       └── UserDto.java              # 现有，需扩展
```

## 复杂性跟踪

> 仅在治理检查有违规需要证明时填写

| 违规 | 为什么需要 | 拒绝更简单替代方案的原因 |
|------|------------|-------------------------|
| 无   | 无         | 无                      |

## 第0阶段 - 研究

### 研究任务

1. **用户表扩展字段设计** - 分析需要添加哪些 GitHub 相关字段
2. **审计日志表结构设计** - 设计安全事件记录表
3. **白名单配置最佳实践** - 研究 Spring Boot 配置模式
4. **现有代码集成点分析** - 确定需要修改的现有代码

### 研究输出

将生成 `research.md` 文档，包含：
- 数据库表结构变更方案
- 配置文件扩展方案
- 现有代码修改点分析
- 安全最佳实践建议

## 第1阶段 - 设计

**前提条件**: `research.md` 完成

### 数据模型生成

基于研究结果，设计：
- 扩展后的 `users` 表结构
- 新的 `audit_logs` 表结构
- 实体类和 DTO 的映射关系

### API 契约生成

定义新增和修改的接口：
- 登出接口
- 用户管理接口
- 审计日志查询接口

### 快速开始场景

关键验证场景：
- GitHub OAuth 完整流程测试
- Token 验证性能测试
- 白名单配置验证

## 实现计划完成

**分支**: 001-github-oauth-enhancement  
**计划**: .kiro/specs/001-github-oauth-enhancement/design.md

### 生成的工件
- design.md - 实现计划
- research.md - 技术决策 (待生成)
- data-model.md - 实体定义 (待生成)
- contracts/ - API 规范 (待生成)
- quickstart.md - 验证场景 (待生成)

### 治理状态
通过 / 无违规

### 准备任务工作流
所有设计工件完成后，可运行 **Tasks** 工作流生成可执行任务列表。