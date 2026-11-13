# 一次性完整部署指南

面向全新环境，从建库到启动一次完成，适用于 Google / GitHub 登录及邮箱白名单版本。主方案为 Linux 单机：Nginx HTTPS + Java 应用 + 本机 MySQL；另附 Windows 启动方法。下面的命令是部署步骤，文档生成不会替你修改线上服务器。

## 1. 部署范围和依赖

整个 Maven 工程只有一个业务启动服务：`beinet-dp-admin`。business、core、sdk 是打包依赖，不需要逐个启动。Vue 2 HTML 保留在 dp-admin 的 `src/main/resources/static` 中，随 JAR 发布，无需 Node、npm 或独立前端站点。

| 组件 | 要求 / 用途 |
| --- | --- |
| Java | JDK 21；构建机和运行机均核对 `java -version` |
| Maven | 构建机安装 3.9.x；仓库无 Maven Wrapper |
| MySQL | 8.0.16 及以上的 8.x，InnoDB、utf8mb4；生产选择仍受维护的版本 |
| Nginx / TLS | 对外 HTTPS，已有可信证书和指向服务器的域名 |
| Redis | 当前 dp-admin 不依赖，不需部署；源码中 Redis 模块仅是可选基础库 |
| 文件存储 | 本机目录或已挂载目录，应用账户可读、上传目录可写 |
| OAuth | Google Web OAuth 客户端和 GitHub OAuth App，至少启用一个 |

流量路径：浏览器 → Nginx `443` → `127.0.0.1:8080` → MySQL `127.0.0.1:3306`。仅开放公网 80/443，8080 和 3306 不对公网开放。服务器需能解析并通过 HTTPS 访问 Google 身份服务及 GitHub 的授权、换票和用户 API；浏览器也需能访问登录供应商。服务器时间需同步。

按实际并发和文件大小规划内存、磁盘和带宽。示例 JVM 上限 1GB，上传单文件上限 1GB；磁盘还要预留 multipart 临时文件、上传文件、数据库、备份和日志空间。

## 2. 配套文件

| 文件 | 用途 |
| --- | --- |
| [01-init-database.sql](sql/01-init-database.sql) | 新安装一次性建库、三张表、索引、外键及检查约束 |
| [02-create-app-user.sql.example](sql/02-create-app-user.sql.example) | DBA 修改密码后创建最小权限运行账户 |
| [application-deploy.yml](deploy/application-deploy.yml) | 完整独立生产配置 |
| [config.ini](deploy/config.ini) | 文件管理目录 |
| [beinet.env.example](deploy/beinet.env.example) | 密钥环境变量模板 |
| [beinet.service](deploy/beinet.service) | Linux systemd 服务 |
| [nginx.conf](deploy/nginx.conf) | 同域名 HTTPS 反向代理 |

所有示例域名、邮箱、密码均需替换。数据库名统一为 `beinet_file_manager`，运行账户为 `beinet_app`。改数据库名时同步修改 SQL 授权范围和 JDBC URL。

## 3. 构建完整应用

在仓库根目录执行：

```shell
java -version
mvn -version
mvn -DskipTests clean package
```

完整 reactor 构建会包含所有已声明模块，部署只取以下可执行文件：

```text
beinet-deployment/beinet-dp-admin/target/beinet-dp-admin-1.0-SNAPSHOT-boot.jar
```

不要使用不带 `-boot` 的普通 JAR。发布前运行隔离认证回归：

```shell
mvn -pl beinet-deployment/beinet-dp-admin -am "-Dtest=OAuthUsersTest,OAuthClientsTest,SessionAuthorizationRequestsTest,AuthorizationFilterTest,OAuthAccountIntegrationTest,AuthApplicationSmokeTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
```

历史 Redis 测试引用原开发环境，故打包命令跳过全量测试；不要直接连接原环境执行它们。若 Maven 私有镜像不可达，用自己的有效 `settings.xml` 配合 `mvn -s /path/settings.xml ...`，不要修改 POM 中模块关系。

## 4. 首次建库与运行账户

先安装并启动 MySQL，配置为仅本机监听，设置独立 DBA 密码。以下从仓库根目录执行，`-p` 交互输入密码，不在命令中明文传密码。

Linux：

```bash
mysql --default-character-set=utf8mb4 -u root -p < docs/sql/01-init-database.sql
# 先把账户模板复制到仓库外，并修改密码，再执行副本：
mysql --default-character-set=utf8mb4 -u root -p < /secure/02-create-app-user.sql
```

Windows PowerShell 不使用上述 `<` 重定向。进入 mysql 客户端后执行（改为实际路径）：

```text
mysql --default-character-set=utf8mb4 -u root -p
mysql> source D:/mySource/beinet-file-manager/docs/sql/01-init-database.sql;
mysql> source D:/beinet-private/02-create-app-user.sql;
```

建库脚本直接创建最终的三张表，字段、索引、外键和检查约束都在各自的 CREATE TABLE 中定义，执行一次即可。数据库必须尚不存在。脚本不会自动执行，需按上面的命令手动导入；不要使用 `--force` 忽略错误。

最终应有：

| 表 | 内容 |
| --- | --- |
| users | 用户资料、状态、软删除标记及 GitHub 信息 |
| oauth_identities | `(provider, subject)` 唯一身份与本地 user_id 映射 |
| audit_logs | 现有审计基础表；建表不代表所有业务操作已接入审计 |

不插入初始管理员或默认密码。白名单用户首次通过 OAuth 验证后自动建立账号；管理员权限由 YML 邮箱配置决定。

验证运行账户 TCP 连接和表结构：

```shell
mysql --protocol=TCP -h 127.0.0.1 -P 3306 -u beinet_app -p beinet_file_manager
```

```sql
SELECT DATABASE(), CURRENT_USER();
SHOW TABLES;
SHOW CREATE TABLE users;
SHOW CREATE TABLE oauth_identities;
SHOW CREATE TABLE audit_logs;
```

运行账户只有本库 SELECT/INSERT/UPDATE/DELETE。如果数据库单独部署，账户 Host 改为应用服务器实际来源地址，并配置数据库 TLS：JDBC 使用 `sslMode=VERIFY_IDENTITY`、正确的数据库 DNS 名和受信任 CA，移除本机模板中的 `allowPublicKeyRetrieval=true`。不要照搬本机非 TLS 连接到公网数据库。

## 5. Google / GitHub 设置

假设正式域名为 `https://files.example.com`：

| 供应商 | 配置 |
| --- | --- |
| Google | 创建 Web application OAuth 客户端，回调 `https://files.example.com/login/oauth2/code/google`；配置 OAuth 同意界面，测试模式添加测试用户 |
| GitHub | 创建 OAuth App，Homepage URL 为站点首页，Authorization callback URL 为 `https://files.example.com/login/oauth2/code/github` |

Google 使用 userinfo.email/userinfo.profile，GitHub 使用 read:user/user:email。域名、协议、端口必须和 `auth.base-url` 一致，不部署到 `/files` 等子路径；前端 API 根路径固定为 `/`。

将双方 Client ID / Secret 填入私有环境变量文件。每个供应商两项都为空时禁用该按钮，仅填一项会启动失败。只允许供应商证明已验证且出现在 `auth.allowed-emails` 中的邮箱；支持 GitHub 已验证的私有邮箱。Google 与 GitHub 使用同一邮箱也会建立独立身份，不自动合并。

修改 YML 中的 `allowed-emails` 和 `admin-emails`。管理员邮箱自动纳入有效登录白名单，无需重复填写；忽略大小写和首尾空白，禁止域名通配符，两个列表均为空时拒绝所有人。修改后重启生效。不要把本人邮箱误写成展示用的 `owner@example.com`。

## 6. Linux 目录、配置与权限

以下使用具有 sudo 权限的部署用户，在仓库根目录执行。创建系统用户的命令仅首次执行：

```bash
sudo useradd --system --home-dir /opt/beinet --shell /usr/sbin/nologin beinet
sudo install -d -o root -g beinet -m 0750 /opt/beinet /etc/beinet
sudo install -d -o root -g beinet -m 0750 /srv/beinet /srv/beinet/read-only
sudo install -d -o beinet -g beinet -m 0750 /srv/beinet/uploads /var/lib/beinet /var/lib/beinet/tmp
sudo install -o root -g beinet -m 0640 beinet-deployment/beinet-dp-admin/target/beinet-dp-admin-1.0-SNAPSHOT-boot.jar /opt/beinet/app.jar
sudo install -o root -g beinet -m 0640 docs/deploy/application-deploy.yml /etc/beinet/application-deploy.yml
sudo install -o root -g beinet -m 0640 docs/deploy/config.ini /etc/beinet/config.ini
sudo install -o root -g root -m 0600 docs/deploy/beinet.env.example /etc/beinet/beinet.env
```

使用 `sudoedit` 编辑上述配置副本和环境变量文件。数据库密码必须与建用户脚本一致。`BEINET_JASYPT_PASSWORD` 设置自己的随机值；模板没有 ENC 密文，之后添加密文时必须使用此密码重新加密。

本方案使用 `--spring.config.location` **替换默认配置位置**，完整配置中显式写明所需项，不加载包内旧数据库、Redis 和 Jasypt 开发配置。不要改为 `additional-location`，否则仍会合并默认配置；此行为见 [Spring Boot 3.4 官方配置说明](https://docs.spring.io/spring-boot/3.4/reference/features/external-config.html)。

`config.ini` 格式为 `key: value`，多个目录用逗号分隔；read-dir/write-dir 都必须非空且实际存在。为只读目录中的文件授予 beinet 组读取和目录遍历权限；上传目录仅授予所需写权限。配置约每 60 秒按修改时间刷新，删除配置文件不会清除旧设置。

systemd 模板启用只读系统目录，只有 uploads/tmp 可写。新增上传目录时，除了修改 config.ini 和文件权限，还要加入服务的 `ReadWritePaths` 并重新加载服务。不要把密钥目录、数据库数据目录或系统根目录作为共享目录。现有路径校验缺陷尚未全部修复，配置的“只读”标签不能代替操作系统权限隔离，见 [分析报告](PROJECT_ANALYSIS.md)。

## 7. HTTPS 代理与服务启动

先准备域名对应的可信 TLS 证书和续期机制，按实际路径修改 Nginx 模板。把配置安装到 Nginx 的 http 上下文目录：

```bash
sudo install -m 0644 docs/deploy/nginx.conf /etc/nginx/conf.d/beinet.conf
sudoedit /etc/nginx/conf.d/beinet.conf
sudo nginx -t
sudo systemctl reload nginx
sudo install -m 0644 docs/deploy/beinet.service /etc/systemd/system/beinet.service
sudo systemctl daemon-reload
sudo systemctl enable --now beinet
sudo systemctl status beinet --no-pager
sudo journalctl -u beinet -n 100 --no-pager
```

核实 `/usr/bin/java` 为 Java 21，否则修改 ExecStart。模板将所有请求转发应用，**不要使用 Nginx alias/root 直接暴露业务 HTML 或共享文件**。不要缓存登录回调或用户响应。Nginx 覆盖转发头，应用仅监听回环地址；如改为跨服务器代理，要用网络规则限制应用端口只接受可信代理连接。

Spring 上传总请求限制与 Nginx 均设置为约 1100MB，为 1GB 文件预留 multipart 开销。提高上限需同步调整两处并检查 tmp 空间。当前日志输出到控制台，由 journald 收集；配置主机日志保留和容量上限。模板关闭访问日志以免记录 OAuth 回调 code，排障时也应避免记录 Cookie、授权码和请求体秘密。

## 8. Windows 部署 / IDEA 本机启动

同样只需一个 JAR 和 MySQL。准备 `D:/beinet/config`、`D:/beinet/data/read-only`、`D:/beinet/data/uploads`、`D:/beinet/tmp`，复制模板到 config。

修改 YML 的 `beinet.config.path` 为 `D:/beinet/config/config.ini`，multipart.location 为 `D:/beinet/tmp`；修改 INI 的两个目录为对应 Windows 路径，使用正斜杠。生产 HTTPS 保留 secure=true 和正式 base-url，代理配置同样要保护全部业务路径。

本机 HTTP 调试专用：base-url 改为 `http://localhost:8080`，cookie.secure 改为 false，供应商登记相同的本机回调。HTTP 仅允许 localhost/127.0.0.1，不能以公网 IP 的 HTTP 地址替代生产域名。

在当前 PowerShell 会话设置模板列出的六个环境变量（通过私有启动配置或安全输入，不提交到仓库）；systemd 的 `.env` 文件不会被 Java 自动读取。然后执行：

```powershell
java -Djava.io.tmpdir=D:/beinet/tmp -jar D:/beinet/app.jar --spring.profiles.active=deploy --spring.config.location=file:D:/beinet/config/application-deploy.yml
```

IDEA 运行 `cn.beinet.deployment.admin.AdminApplication` 时，将两个 `--spring...` 参数填入 Program arguments，将密钥填入 Environment variables，并使用 JDK 21。正式长期运行应由 Windows 服务管理器托管 Java，配置工作目录、环境变量、日志轮转和失败重启；普通 PowerShell 前台启动关闭窗口即停止。NTFS 权限应限制到独立服务账户，不用管理员身份运行应用。

## 9. 上线验收

```shell
curl -I https://files.example.com/login.html
curl -i https://files.example.com/auth/providers
curl -i https://files.example.com/auth/session
curl -I https://files.example.com/stores/store.html
curl -i https://files.example.com/stores/list
```

按顺序预期：登录页 200、供应商列表 200 且包含已配置供应商、Session 401、业务 HTML 302 跳登录、业务 API 401。`/auth/providers` 只能作为 HTTP 存活探测，不能证明数据库和真实 OAuth 可用；健康检查接口也不应匿名放行。

用浏览器分别验收 Google 和 GitHub：

1. 白名单已验证邮箱能登录；非白名单拒绝。
2. 普通用户可列目录、上传、下载、预览，但不能访问用户管理等管理员接口。
3. 管理员可进入用户管理；禁用测试用户后其现有会话不可再访问业务。
4. 上传小文件及接近设定上限的测试文件，下载核对内容，检查视频拖动行为。
5. 退出后原会话不能访问 API；不带 CSRF 的写请求返回 403。
6. HTTPS 会话 Cookie 为 Secure、HttpOnly、SameSite=Lax；等待/重启后需重新登录。

生产凭据和真实供应商登录需在目标环境完成以上验收。本地测试不能替代此步骤。

## 10. 日常备份与恢复

备份需要同时覆盖数据库三张表、共享文件、配置、OAuth 应用信息及密钥（密钥备份加密保管）。暂停写入后做一致备份，并将备份放在文件管理不可访问的位置：

```bash
mysqldump -u root -p --single-transaction --no-tablespaces --databases beinet_file_manager > /secure/backup/beinet-backup.sql
```

备份期间不要并行执行 DDL。定期在隔离数据库恢复备份并验证身份映射和文件可读；配置文件本身不包含上传文件内容，不能只备份数据库。

恢复时停止服务，将数据库、文件、配置和对应 JAR 恢复到同一备份时间点，再启动并完成验收。恢复备份会丢失备份后的写入，应事先确定恢复时间点。

当前会话位于进程内存中，重启全部失效。此文采用单实例部署；不能直接增加实例并随机分流，否则授权 state 和登录会话可能丢失。高可用需要另行实现共享 Session 或经过验证的粘性会话方案。

## 11. 常见故障

### 可选云工具

所有模块已包含在 JAR 内，但阿里云安全组和 AWS 邮件工具需要你自己的云资源与凭据，不能通过建库自动开通。不使用这些功能时不必填写；Google/GitHub 登录也不依赖它们。使用时在外部 YML 追加对应配置，并将环境变量加入私有服务环境文件：

```yaml
ali:
  security-policy:
    region-id: ${ALI_REGION_ID}
    group-id: ${ALI_SECURITY_GROUP_ID}
    access-key-id: ${ALI_ACCESS_KEY_ID}
    access-key-secret: ${ALI_ACCESS_KEY_SECRET}
aws:
  email:
    from: ${AWS_EMAIL_FROM}
    region: ${AWS_EMAIL_REGION}
    access-key: ${AWS_EMAIL_ACCESS_KEY}
    secret-key: ${AWS_EMAIL_SECRET_KEY}
```

阿里云配置对应现有安全组工具，AWS 配置对应 SES 邮件工具，不是文件存储后端。云资源需独立开通并配置所需权限；发信身份和账号发送限制按云账户实际状态验证。不要把历史 `github.client-id` 或 `github.oauth.*` 配置当作新登录配置，新登录只读 `auth.github.*`。旧演示、在线解密、RAR 解压及目录提级接口仍按认证策略禁用，不因“完整部署”重新开放。

### 排障对照

| 现象 | 优先检查 |
| --- | --- |
| 启动仍连原开发数据库 | 是否用了本文 `spring.config.location`；是否有环境变量/命令行覆盖配置 |
| 登录页没有按钮 | 对应 Client ID / Secret 是否均为空、环境变量是否进入服务进程 |
| redirect_uri_mismatch / 回调失败 | base-url、登记回调、访问域名是否一致；是否 5 分钟超时、重复点登录覆盖 state |
| HTTP 本机登录循环 | 本机 secure 是否误为 true；localhost 和 127.0.0.1 是否混用 |
| 登录失败且邮箱正确 | 供应商邮箱是否验证、Google 测试用户、账号是否禁用/软删除、数据库是否可写 |
| 业务 503 | 数据库连通性、权限、连接池；鉴权依赖数据库，故障时不会放行 |
| 写操作 403 | CSRF Cookie/Header、会话过期、管理员权限；不要通过关闭 CSRF 修复 |
| 上传 413 / 超时 | Nginx 与 Spring 双重限额、代理超时、临时目录空间和权限 |
| 文件列表为空 / 500 | INI 路径、冒号格式、两个目录存在且非空、服务账户权限、系统沙箱限制 |
| 缺表 / 数据库已存在 | 核查初始化执行结果及 JDBC 数据库名；本脚本仅用于空环境，不要重复导入 |
| MySQL Access denied | 运行账户密码、来源 Host、TCP 登录测试；不要直接改用 root |

认证细节见 [AUTHENTICATION.md](AUTHENTICATION.md)。SQL 的账户创建与授权语法参考 [MySQL 官方文档](https://dev.mysql.com/doc/refman/8.4/en/create-user.html)。


## 跨域访问（CORS）

在现有 `auth` 配置下添加以下内容，只填写可信前端的来源（协议、主机、端口），不带路径或结尾斜杠，不支持 `*`：

```yaml
auth:
  cors:
    allowed-origins:
      - https://app.example.com
      - http://localhost:8081
```

默认列表为空，仅信任 `auth.base-url` 对应的本站来源。允许跨域意味着该前端可以使用用户的登录会话读取数据，请勿加入不可信网站。

AuthorizationFilter 在登录校验前处理真正的预检（OPTIONS 同时携带 Origin 和 Access-Control-Request-Method）。来源、方法、请求头均合法时直接返回 200，不创建 Session、不查数据库、不进入 Controller。非法预检返回 403。普通 OPTIONS 仍需登录；预检成功不表示实际业务请求已获授权。TRACE/CONNECT 保持 405。

允许的方法：GET、HEAD、POST、PUT、PATCH、DELETE、OPTIONS。允许的请求头：Content-Type、X-XSRF-TOKEN、Range。实际请求继续检查 Session、邮箱名单、账号状态、管理员权限；写请求还检查 Session CSRF 值。可信来源的 401/403/503 响应也带跨域响应头，方便前端处理。

前端使用 `fetch(url, {credentials: 'include'})`，或 Axios 的 `withCredentials: true`。登录后先请求 `/auth/session`，从响应头 `X-XSRF-TOKEN` 读取值，再放入写请求的同名请求头。不要尝试读取另一个域名的 Cookie。文件响应还暴露 Content-Disposition、Content-Range、Accept-Ranges。OAuth 登录应通过浏览器顶层跳转进入后端登录入口，不使用跨域 AJAX 跟随供应商授权页面。

当前 Session Cookie 使用 SameSite=Lax：同站点不同端口或同站点子域名的跨源请求可使用；不同站点的跨站 AJAX 仍受 Cookie 策略限制。确需跨站部署时，要使用 HTTPS 并将 `server.servlet.session.cookie.same-site` 配置为 `none`、`secure` 配置为 `true`，前端通过上述响应头取得 CSRF 值；浏览器禁用第三方 Cookie 时仍可能不可用，优先采用同站点域名或反向代理。

协议说明：[MDN CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS)。
