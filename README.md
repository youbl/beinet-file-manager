# Beinet File Manager

基于 Java 21、Spring Boot 和 Vue 的服务器本地文件管理工具。通过浏览器浏览配置的目录、上传和下载文件，并预览图片与视频。项目同时包含用户管理、第三方登录和部分运维工具。

> **当前版本仅适合隔离环境评估。** 认证失败后请求仍被放行，上传和路径处理存在越权风险。切换到 `prod` 或更换 JWT 密钥不能消除这些问题。请先阅读 [项目分析与安全报告](docs/PROJECT_ANALYSIS.md)，不要直接暴露到公网，也不要使用真实云凭据或挂载重要目录。

## 功能与状态

| 功能 | 当前实现与限制 |
| --- | --- |
| 文件浏览 | 多个只读、读写根目录；展示文件名、大小和修改时间 |
| 上传 | 写入配置目录，同名文件通常拒绝覆盖；默认请求上限 10 GB，建议调小 |
| 下载与预览 | 普通下载、单段 Range、图片和视频预览；Range 有已知边界错误 |
| 实时配置 | 每 60 秒检查一次 config.ini 修改时间 |
| 用户管理 | 查询、新增、修改、删除；权限与敏感字段隔离不完善 |
| 第三方登录 | GitHub 授权码、Google Access Token 接口；配置及会话存在已知问题 |
| 运维工具 | 阿里云安全组、AWS SES 邮件、摘要计算、配置加解密 |
| 文件整理 | RAR 批量解压、单子目录提级；会修改文件，RAR 成功后删除源压缩包 |
| 未完成部分 | 文件删除按钮隐藏且对应 API 缺失；审计查询、白名单管理主要接口被注释 |

这是 Maven 多模块、单个 Spring Boot 部署入口的应用。文件保存在服务端本地文件系统，不是完整的多服务文件存储平台。

## 技术栈与结构

- Java 21、Spring Boot 3.4.5、Spring Cloud 2024.0.1。
- MyBatis-Plus、MySQL、OpenFeign、Hutool JWT、Jasypt。
- 静态 HTML、Vue、Element UI、Axios、DPlayer，无需 Node.js/npm 构建。
- Redis 工具模块独立存在，管理端 POM 中对它的依赖已注释。

```text
beinet-file-manager/
├── beinet-business/
│   ├── beinet-login/             # 登录、用户持久化、审计
│   └── beinet-business-base/     # 空基础模块，未加入父 POM 的 modules
├── beinet-core/
│   ├── beinet-base/              # DTO、常量、异常、基础配置
│   ├── beinet-dao/               # 数据访问依赖
│   ├── beinet-feign/             # HTTP 客户端及日志
│   ├── beinet-redis/             # 缓存、分布式锁
│   ├── beinet-third/             # GitHub、Google、阿里云、AWS
│   ├── beinet-utils/             # 文件、JWT、IP 等工具
│   └── beinet-web/               # Web 上下文、异常处理
├── beinet-sdk/
│   ├── beinet-event-sdk/         # 文件事件记录
│   └── beinet-login-sdk/         # 登录接口和 DTO
├── beinet-deployment/beinet-dp-admin/
│   └── src/main/
│       ├── java/                # 启动类、管理接口、文件服务
│       └── resources/
│           ├── static/          # 前端页面和第三方静态资源
│           └── db/migration/    # 增量 SQL，缺少基础 users 建表
├── config.ini                   # 文件目录与磁盘活动配置
└── docs/PROJECT_ANALYSIS.md      # 缺陷、证据、修复建议与验证记录
```

## 本地构建

准备 JDK 21、Maven（本次检查环境为 3.9.9）以及可访问的 Maven 仓库。仓库没有 Maven Wrapper。从项目根目录执行：

```shell
java -version
mvn -version
mvn -DskipTests package
```

按 POM 配置，启动产物为：

```text
beinet-deployment/beinet-dp-admin/target/beinet-dp-admin-1.0-SNAPSHOT-boot.jar
```

请使用带 `-boot` 的 JAR。`-DskipTests` 跳过测试执行，仍编译测试代码。本次离线构建因本机 Maven 镜像缺少 Spring Boot 父 POM 缓存而终止，尚未验证完整打包成功。

## 启动前准备

### 1. 准备隔离数据库

登录、用户管理和审计使用 MySQL。仓库目前**不能从空数据库直接完成完整初始化**：

- `V001__extend_users_table_for_github_oauth.sql` 对已有 users 表执行 ALTER TABLE。
- `V002__create_audit_logs_table.sql` 创建审计表，外键依赖 users(id)。
- POM 未集成 Flyway，文件放在 db/migration 中不会自动执行。

已有兼容旧表时，先备份隔离库，核对后依次执行两个脚本；脚本不保证重复执行安全。新使用者需先补齐与 [Users 实体](beinet-business/beinet-login/src/main/java/cn/beinet/business/login/dal/entity/Users.java) 一致的基础表结构。不要连接生产数据库进行演示。

### 2. 使用外部 Spring 配置

仓库配置含原开发环境地址及密文，需要覆盖为自己的隔离配置。将下面的示例保存到仓库外的 `application-local.yml`，替换路径并设置环境变量：

```yaml
server:
  address: 127.0.0.1
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/beinet_demo?serverTimezone=UTC&characterEncoding=utf8
    username: beinet_demo
    password: ${BEINET_DB_PASSWORD}
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB

beinet:
  config:
    path: D:/beinet-demo/config.ini

login:
  secret: ${BEINET_LOGIN_SECRET}

jasypt:
  encryptor:
    password: ${BEINET_JASYPT_PASSWORD}

# GitHub 功能需要时再配置，这是代码实际读取的键。
github:
  client-id: ${GITHUB_CLIENT_ID:}
  client-secret: ${GITHUB_CLIENT_SECRET:}
```

设置 `BEINET_DB_PASSWORD`、`BEINET_LOGIN_SECRET`、`BEINET_JASYPT_PASSWORD`。后两者使用独立随机秘密，不要提交到 Git。数据库账户仅授予演示库必要权限。

更换 Jasypt 主密码后，所有仍使用的 `ENC(...)` 配置都必须重新加密或覆盖，不能沿用仓库密文。保存在 Git 中的主密码不具备保密性。

### 3. 准备目录配置

创建两个仅含演示文件的目录，将以下内容保存为上面指定的 `D:/beinet-demo/config.ini`：

```ini
enabled-file-manager: true
read-dir: D:/beinet-demo/read-only
write-dir: D:/beinet-demo/uploads

# 可选：每十分钟向指定分区追加 beinet.txt，评估时无需启用。
# active-disks: D
```

格式为 `key: value`，不是传统分节 INI；多个目录用逗号分隔，建议使用绝对路径和正斜杠。目录需预先存在，否则被忽略。

**目前启用文件管理时，read-dir 和 write-dir 都应配置非空值**，空配置存在空指针问题。读写目录也可读取。配置目录不能替代操作系统权限隔离，服务账号也应仅能访问演示目录。`enabled-file-manager: false` 不会禁用目录提级和 RAR 接口。

显式设置 `beinet.config.path` 可避免 IDE、命令行和系统服务的默认路径差异。文件修改时间变大后通常在下一次 60 秒轮询加载；删除配置文件不会清除已有配置。

### 4. 启动并访问

从项目根目录执行，替换外部配置路径：

```shell
java -jar beinet-deployment/beinet-dp-admin/target/beinet-dp-admin-1.0-SNAPSHOT-boot.jar --spring.profiles.active=local --spring.config.additional-location=file:D:/beinet-demo/application-local.yml
```

这里的 local 用于选择个人配置，**不是安全开关**。应用启动后可访问：

| 页面 | 地址 |
| --- | --- |
| 首页 | `http://127.0.0.1:8080/` |
| 文件管理 | `http://127.0.0.1:8080/stores/store.html` |
| 用户管理 | `http://127.0.0.1:8080/users/users.html` |
| Swagger UI | `http://127.0.0.1:8080/swagger-ui/index.html` |

前端 `res/common.js` 固定 API 根路径为 `/`，子路径部署需同步修改。RAR 工具硬编码依赖 Windows 的 `C:\Program Files\WinRAR\Rar.exe`，目录提级也包含 Windows 路径拼接，这两项尚不是跨平台功能。

## 登录与可选集成

当前没有开箱即用的完整登录向导：

- GitHub 登录接口为 `GET /login/github?code=...`，实际读取 `github.client-id/client-secret`，不是主 YAML 中的 `github.oauth.*`。
- 默认配置的 `/login/github/callback` 没有对应处理方法。演示页 `/demo/github.html` 中的 Client ID 和回调地址需替换；其回调调用 `/github/callback` 仅获取资料，不签发本地登录 Cookie。
- GitHub 未公开邮箱的用户可能登录失败，OAuth state 校验尚未完成。
- `GET /login/google?accessToken=...` 可签发 JWT，但 Google 用户尚未持久化到本地用户表。
- Cookie 为 `HttpOnly; Secure; SameSite=None`，普通 HTTP 主机可能无法保存/发送 Cookie；正式登录联调应配置 HTTPS。
- JWT 实际读取 `login.secret`，有效期在代码固定为 30 天；`auth.jwt.expire-seconds` 当前不控制签发时长。

阿里云安全组使用 `ali.security-policy.*`，AWS SES 使用 `aws.email.*`。评估文件管理无需配置真实云凭据；修复访问控制前不要连接真实云账号。

## 文件 API

| 方法 | 路径 | 参数 |
| --- | --- | --- |
| GET | `/stores/list` | 可选 dir，不传返回配置根目录 |
| GET | `/stores/status` | dir，返回目录信息及只读状态 |
| POST | `/stores/uploadFile` | 查询参数 dir，multipart 字段 file |
| GET | `/stores/download` | file，可带 Range 请求头 |

普通 JSON 接口使用 code、msg、data，code=0 表示成功；下载返回文件流。部分失败仅写业务码或日志，不能仅用 HTTP 200 判断成功。目录提级与解压有移动、覆盖、删除副作用，不作为日常文件 API 推荐使用。

## 测试与贡献

目前测试主要覆盖基础枚举、IP 工具和 Redis，缺少核心文件与认证回归测试。Redis 测试指向原开发环境并写入固定键，运行前应替换为独立本地测试服务。还应核对 JUnit 引擎是否实际发现了测试。

隔离测试配置后可执行 `mvn test`，检查 Surefire 报告中的用例数。构建或测试成功不代表报告中的安全问题已解决。

欢迎提交 Issue/PR，请提供 Java/Maven 版本、操作系统、复现步骤及脱敏日志。修改业务逻辑时附上预期行为的回归测试；不要在 Issue 中公开密码、Token、云凭据或真实文件数据。

## 许可证

项目采用 [Apache License 2.0](LICENSE)，第三方前端资源仍需遵循各自许可证。更多缺陷与修复顺序见 [分析报告](docs/PROJECT_ANALYSIS.md)。
