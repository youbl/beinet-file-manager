# Beinet File Manager

基于 Java 21、Spring Boot 和 Vue 2 的本地文件管理工具。HTML、JS 和样式随 `beinet-dp-admin` 发布，不需要独立前端服务或 npm 构建。

支持 Google / GitHub 登录，**只有 YML 邮箱白名单中的已验证邮箱可以进入系统**。未登录用户仅能访问登录入口和静态资源；文件列表、上传、下载、预览及业务 HTML 均受后端保护。

> 本次已重构认证与会话，但原有文件路径校验、Range 等问题仍需处理。请使用最小文件系统权限，勿将敏感目录配置为可写。见 [历史分析报告](docs/PROJECT_ANALYSIS.md) 与 [本次认证说明](docs/AUTHENTICATION.md)。

## 功能

| 功能 | 说明 |
| --- | --- |
| 登录 | Google / GitHub OAuth2 授权码流程，PKCE、会话绑定 state |
| 邮箱准入 | 精确匹配、忽略大小写和首尾空白；不支持域名或通配符；两个列表均为空时拒绝所有人 |
| 会话 | 服务端 Session + HttpOnly Cookie，默认闲置 8 小时过期，退出立即失效 |
| 文件 | 多根目录浏览、上传、下载、图片和视频预览 |
| 管理员 | 单独配置管理员邮箱，保护用户管理、云操作和配置工具 |
| 禁用用户 | 每次受保护请求核对用户状态；禁用/删除后不能继续访问 |
| 已停用入口 | 旧 JWT 登录、旧第三方回调演示、在线解密、RAR 解压和目录提级 |

普通白名单用户共享 config.ini 配置的文件目录；当前没有每用户独立目录。管理员邮箱自动纳入有效登录白名单，无需在 allowed-emails 中重复填写。YML 修改需要重启，重启也会清除现有本机会话。

## 技术栈与结构

Java 21、Spring Boot 3.4.5、普通 Servlet Filter、HttpSession 和 OAuth2 HTTP 客户端、MyBatis-Plus、MySQL；前端使用 Vue 2、Element UI、Axios 和 DPlayer。

```text
beinet-business/beinet-login/       # 用户、OAuth 身份持久化、审计基础代码
beinet-core/                       # Web、文件、Feign、第三方及基础工具
beinet-sdk/                        # DTO、事件与历史接口定义
beinet-deployment/beinet-dp-admin/
  src/main/java/.../auth/           # 认证链、供应商、邮箱策略、Session 接口
  src/main/resources/
    static/login.html              # Vue 2 登录页面
    static/stores/store.html       # 文件管理页面（需登录）
    static/users/users.html        # 用户管理页面（需管理员）
    static/res/                    # 可公开访问的静态资源
    db/bootstrap.sql               # 空库基础结构
    db/migration/                  # 手动增量迁移
config.ini                         # 文件目录配置
docs/AUTHENTICATION.md              # 认证设计、接口、迁移与验证说明
```

## 构建

准备 JDK 21 和 Maven 3.9.x，在项目根目录执行：

```shell
mvn "-DskipTests" package
```

启动产物为：

```text
beinet-deployment/beinet-dp-admin/target/beinet-dp-admin-1.0-SNAPSHOT-boot.jar
```

使用带 `-boot` 的 JAR。仓库没有 Maven Wrapper；确保本机 Maven 仓库镜像可用。

## 配置与启动

完整的 Linux / Windows 部署步骤、HTTPS 代理、OAuth 设置、备份恢复及配套建库 SQL 见 **[完整部署指南](docs/DEPLOYMENT.md)**。首次部署建议直接使用其中的独立生产配置模板。

### 1. 初始化数据库

首次安装使用 [完整建库 SQL](docs/sql/01-init-database.sql)，直接创建最终数据库、表、索引和约束，执行一次即可。随后按 [运行账户模板](docs/sql/02-create-app-user.sql.example) 设置自己的数据库账户与密码。SQL 需手动执行，详细命令见 [一次性完整部署指南](docs/DEPLOYMENT.md)。

### 2. 注册 OAuth 应用

在 Google 配置 Web application OAuth 客户端，在 GitHub 配置 OAuth App。假设站点地址是 `https://files.example.com`，登记的回调必须分别为：

```text
https://files.example.com/login/oauth2/code/google
https://files.example.com/login/oauth2/code/github
```

本机调试可使用 `http://localhost:8080` 对应的回调。浏览器访问地址、`auth.base-url` 和供应商登记回调必须一致，不能混用 localhost / 127.0.0.1。

Google 登录请求 userinfo.email、userinfo.profile；GitHub 请求 read:user、user:email，以读取私有邮箱的验证状态。Google 测试模式还需在供应商控制台配置测试用户。

### 3. 外部 YML 示例

不要修改或复用仓库里的原开发环境数据库凭据。在仓库外创建 `application-local.yml`：

```yaml
server:
  address: 127.0.0.1
  port: 8080
  servlet:
    session:
      timeout: 8h
      cookie:
        secure: false # 仅本机 HTTP；HTTPS 部署必须为 true
        http-only: true
        same-site: lax

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

auth:
  base-url: http://localhost:8080
  allowed-emails:
    - owner@example.com
    - colleague@example.com
  admin-emails:
    - owner@example.com
  google:
    client-id: ${GOOGLE_CLIENT_ID:}
    client-secret: ${GOOGLE_CLIENT_SECRET:}
  github:
    client-id: ${GITHUB_CLIENT_ID:}
    client-secret: ${GITHUB_CLIENT_SECRET:}

jasypt:
  encryptor:
    password: ${BEINET_JASYPT_PASSWORD}
```

替换示例邮箱并设置环境变量。供应商 ID 和 Secret 都留空时隐藏该登录方式；只填一项会阻止启动。默认 allowed-emails/admin-emails 都为空，不会自动批准首个用户或自动授予管理员权限。

`auth.base-url` 必须是 HTTPS origin，仅允许 localhost/127.0.0.1 使用 HTTP。生产 Cookie 默认 Secure；本机 HTTP 需显式设为 false。旧 `login.secret`、`auth.jwt.*`、`github.oauth.*`、`auth.whitelist.*` 不再控制登录。现有 JWT/Cookie 不兼容新会话，升级后需重新登录。

如果更换 Jasypt 主密码，所有仍使用的 ENC 值都必须重新加密或覆盖；不要保留无法解密的旧配置。阿里云与 AWS 集成可不配置，只有使用对应功能时才需设置独立凭据。

### 4. 文件目录

预先创建两个演示目录，将下面配置保存至 `beinet.config.path` 指定位置：

```ini
enabled-file-manager: true
read-dir: D:/beinet-demo/read-only
write-dir: D:/beinet-demo/uploads
# 可选：每 10 分钟向分区追加 beinet.txt。通常无需启用。
# active-disks: D
```

格式为 `key: value`，多目录用逗号分隔。不存在的目录会被忽略。当前目录配置仍有空列表缺陷，启用时两项均需非空。配置每 60 秒检查一次；修改时间增大时刷新，删除文件不会清除旧配置。

### 5. 启动

```shell
java -jar beinet-deployment/beinet-dp-admin/target/beinet-dp-admin-1.0-SNAPSHOT-boot.jar --spring.profiles.active=local --spring.config.additional-location=file:D:/beinet-demo/application-local.yml
```

打开 `http://localhost:8080/`，未登录会进入登录页。登录成功返回原业务 HTML，默认文件管理页。业务页面顶部显示当前邮箱和退出按钮。

应用采用同源部署，API 根路径固定为 `/`。当前 Session 存在单个应用内存中，重启失效；多实例部署前需要共享 Session 或明确的粘性会话方案。HTTPS 代理部署时对外 origin 必须与 auth.base-url 一致。

## 常用接口

| 方法 | 路径 | 访问要求 |
| --- | --- | --- |
| GET | /auth/providers | 公开，仅返回启用的供应商名称 |
| GET | /oauth2/authorization/google 或 github | 公开，发起授权 |
| GET | /login/oauth2/code/google 或 github | 公开，必须通过 state/供应商校验 |
| GET | /auth/session | 登录后，返回本地 ID、邮箱、供应商和管理员标记 |
| POST | /auth/logout | CSRF 校验，销毁本地会话 |
| GET | /stores/list、/stores/status | 登录后，dir 查询参数 |
| POST | /stores/uploadFile | 登录后，dir 查询参数 + multipart 字段 file + CSRF Header |
| GET | /stores/download | 登录后，file 查询参数，可带 Range |
| 多种 | /users、/api/aliyun/**、/aws/**、/tools/**、/config/** | 管理员；在线解密仍拒绝 |

未登录 API 返回 HTTP 401，业务 HTML 跳转登录；权限不足或 CSRF 无效返回 403。Swagger、健康检查、调试接口也需要登录，没有 dev 免登录例外。

写请求携带 Cookie 和 `X-XSRF-TOKEN`，值取自同源 `XSRF-TOKEN` Cookie。Axios 默认支持，Element UI 上传已单独适配。认证 Cookie 为 HttpOnly，JS 可读取的 CSRF Cookie 不是登录凭据。

## 测试

认证相关测试不连接真实 OAuth 服务，数据库测试使用隔离 H2 MySQL 模式：

```shell
mvn -pl beinet-deployment/beinet-dp-admin -am "-Dtest=OAuthUsersTest,OAuthClientsTest,SessionAuthorizationRequestsTest,AuthorizationFilterTest,OAuthAccountIntegrationTest,AuthApplicationSmokeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

历史 Spring 版本的测试数量不作为当前版本证据；当前验证见需求目录技术文档。测试覆盖邮箱验证、身份持久化和事务回滚、禁用账号、匿名 API/页面、state 过期/错配/重放、CSRF、管理员、退出、回跳地址和实际应用资源加载。真实 Google/GitHub 登录需配置自己的凭据与回调后联调；H2 验证不能代替目标 MySQL 版本上的迁移演练。

仓库旧 Redis 测试使用原开发环境与固定键，不要直接运行全量测试连接原环境。

## 许可证

采用 [Apache License 2.0](LICENSE)，第三方静态资源遵循各自许可证。反馈问题时附复现步骤和脱敏日志，不要提交密钥、Token 或真实文件内容。

## 需求与技术设计

本次改动的完整需求、验收标准及认证流程 UML 见 [OAuth 登录与全站访问控制需求目录](docs/requirements/oauth-access-control/README.md)。

业务访问控制统一位于 `beinet-deployment/beinet-dp-admin/src/main/java/cn/beinet/deployment/admin/autoConfig/AuthorizationFilter.java`，按编号逐项判断公开路径、登录、邮箱与账号状态、停用接口和管理员权限。OAuth 和退出由普通 Controller 处理，本站身份使用 HttpSession；CSRF 由过滤器校验，已移除 Spring Security。


### 跨域前端

通过 `auth.cors.allowed-origins` 配置可信来源，默认不开放外部跨域。合法 OPTIONS 预检返回 200，实际业务请求仍需登录和权限校验。配置及前端携带 Cookie/CSRF 的方式见 [认证文档](docs/AUTHENTICATION.md#跨域访问cors)。
