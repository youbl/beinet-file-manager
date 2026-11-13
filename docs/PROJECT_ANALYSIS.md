# 项目分析与安全审查报告

> 更新（2026-09-14）：下文保留 2026-09-12 的历史审查基线。本次已重构 Google/GitHub 登录、邮箱白名单、Session 与默认拒绝策略，并停用旧登录/解密/文件整理入口；新增基础建表和身份迁移。当前实现与验证见 [认证说明](AUTHENTICATION.md)。原过滤器、验证器和 LoginService 等已删除，相关旧位置以文件名保留。历史报告未涉及的或未修复的问题不能视作已解决。

审查日期：2026-09-12。代码基线：`318525b` 加当前工作区。开始审查时 `application-dev.yml`、`application-prod.yml` 已处于暂存删除状态，本次保留这两项变更。

## 1. 总体结论与范围

当前项目已具备文件管理主流程，但不具备安全对外部署的条件。最严重的问题是认证失败后仍放行、上传目标未经完整校验、路径清理存在绕过，以及高副作用工具不检查目录权限。仅切换 profile 或替换 JWT 密钥不能解决这些问题。

审查覆盖 Maven 模块、配置、文件服务、认证、OAuth、用户 CRUD、SQL 迁移、审计、云接口、前端调用及测试。未进行全量依赖 CVE 扫描，不把旧版本直接等同于可利用漏洞，也不宣称不存在其他安全问题。

证据区分为**静态确认**、**隔离逻辑验证**和**待集成验证**。本次没有启动应用连接配置中的数据库，没有解密凭据，没有执行真实上传、删除、移动、云操作或邮件发送。以下漏洞已记录，但尚未修改业务代码修复。

## 2. 架构与能力评估

`beinet-dp-admin` 是唯一部署入口，扫描 cn.beinet，启用异步和调度，聚合业务与第三方模块；前端静态 HTML 随 JAR 发布，无 npm 构建。业务基础目录 beinet-business-base 仅有空 POM，未加入父模块构建。

```text
浏览器 → AuthorizationFilter → Validator 列表 → Controller
                                           ├─ StoreService → RealConfigVar → 本地文件系统
                                           ├─ LoginService → GitHub/Google → users / JWT Cookie
                                           ├─ UsersService → MyBatis-Plus → MySQL
                                           └─ 云工具 → 阿里云 / AWS SES
RealConfig → 每 60 秒检查 config.ini
StoreJobs → 每 10 分钟向配置分区追加 beinet.txt
```

已有合理设计包括服务层集中处理文件、正常上传拒绝覆盖、JWT 验签和日期校验、Cookie 的 HttpOnly/Secure、SQL 多数使用绑定参数、GitHub ID 唯一约束。但更外层的失败放行和不完整路径检查使这些保护不足以构成安全边界。

功能并不完整：缺初始数据库结构，文件删除按钮隐藏且 API 缺失，审计查询和白名单管理主要处理方法被注释。不能根据接口注释认定功能已经开放。Redis 是独立工具模块，当前管理端 POM 对其依赖已注释。

## 3. 缺陷总览

P0：对外部署前必须处理；P1：高风险安全或核心功能缺陷；P2：正确性与工程质量问题。这是工程优先级，不是 CVSS。多项可能属于同一攻击链，不应累加计算影响。

| 编号 | 优先级 | 问题 | 证据 |
| --- | --- | --- | --- |
| S01 | P0 | 认证失败仍放行，所有环境都有绕过路径 | 静态确认 |
| S02 | P0 | 上传原始文件名可越过允许目录 | 静态＋逻辑验证 |
| S03 | P0 | 清理顺序留下父级路径；缺真实路径边界校验 | 清理逻辑已验证，链接场景待集成 |
| S04 | P0 | 解压和目录提级不检查目录权限及总开关 | 静态确认 |
| S05 | P0 | 密文与主密码同库，在线解密接口暴露 | 静态确认 |
| S06 | P1 | JWT 默认秘密公开，配置键和有效期不一致 | 静态确认 |
| S07 | P1 | 用户管理无角色/租户授权，敏感字段输出与批量赋值 | 静态确认 |
| S08 | P1 | 任意 Origin 可获凭据跨域授权，缺 CSRF 防护 | 静态，浏览器利用条件待验证 |
| S09 | P1 | OAuth 缺 state 会话绑定，日志记录凭据 | 静态确认 |
| S10 | P1 | 登出不吊销 Token，禁用用户仍可获 Token | 静态确认 |
| B01 | P1 | 缺初始 users 建表与自动迁移集成 | 静态确认 |
| B02 | P1 | GitHub 配置、回调、邮箱及 Google 持久化不完整 | 静态确认 |
| B03 | P1 | 空目录配置触发空指针 | 静态确认 |
| B04 | P2 | 目录列表 readonly 取反错误 | 静态确认 |
| B05 | P2 | Range 边界处理错误，下载异常被吞掉 | 静态＋逻辑验证 |
| B06 | P2 | 白名单 enabled 配置未被验证器采用 | 静态确认 |
| B07 | P2 | 异步文件任务状态竞争、进程控制及跨平台缺陷 | 静态，运行条件待集成 |
| B08 | P2 | 审计 SQL 注解、异步调用、数据库约束不一致 | 静态，数据库行为待验证 |
| B09 | P2 | 用户登录类型迁移回填条件错误 | 静态确认 |

## 4. 安全问题详情

### S01：认证失败仍执行请求

**位置**：AuthorizationFilter.java（历史文件），doFilterInternal，约 50–85 行。

所有验证器失败后，dev 分支设置 Dev_Debug 放行；非 dev 分支同样调用 filterChain.doFilter，401 处理被注释。因此匿名请求可到达没有自行鉴权的文件、用户、解密和云接口。部分 login 接口自行验 Token，不能保护其他 API。云操作是否成功还取决于部署凭据及云权限。

**修复**：删除所有失败放行逻辑，在所有环境统一返回 HTTP 401；认证成功但授权不足返回 403。不能只删除末尾放行而保留 dev 绕过。

**验收**：隔离环境不带 Cookie/Authorization 访问 /stores/list、/users?id=1 应为 401，业务服务不被调用；有效用户需进一步通过资源权限校验。

### S02：上传文件名可绕过目录限制

**位置**：[StoreService.java][store]，uploadFile，108–118 行；[FileHelper.java][file]，83–93 行。

代码只检查 dir，再把客户端 getOriginalFilename() 直接拼接到 dir，底层创建父目录并写文件。允许根为 D:/sandbox/allowed/ 时，原始 multipart 文件名 ../outside.txt 的最终目标位于父目录。普通浏览器选文件通常只发基础名称，不代表服务端可以信任其他客户端。

**影响**：可在操作系统进程权限范围内创建目录外文件。现有文件一般被拒绝覆盖，因此不能直接声称任意覆盖；进一步影响取决于可写位置。当前 S01 使攻击者不必先登录。

**验证**：隔离 Java 路径计算确认拼接后 normalize() 不在允许根；未实际发送 multipart 请求。

**修复/验收**：拒绝文件名中的路径分隔符、父级段和平台特殊路径，对最终目标而非仅 dir 授权，检查真实父目录，并以 CREATE_NEW 原子创建；覆盖 ../、反斜杠、重名、并发、符号链接目录测试。

### S03：路径清理顺序漏洞及链接边界缺失

**位置**：[FileHelper.java][file]，101–133 行；[RealConfigVar.java][dirs]，65–93 行。

clearFileName 先移除 ../ 和末尾 ..，最后才 trim。输入 `D:/sandbox/allowed/.. `（末尾空格）得到 `D:/sandbox/allowed/..`，clearDirName 再补斜杠。路径仍以允许根开头，实际文件系统却访问父目录。本次提取实际清理方法运行，确认清理后仍存在父级段；未执行 HTTP 枚举。

此外，字符串 startsWith 不解析符号链接或 Windows junction。如果允许根中存在指向外部的链接，也可能越界。本次未创建链接验证，利用需要相应文件系统条件。配置根有尾部斜杠，不能把本问题误报为简单的 allowed2 同名前缀绕过。

**修复/验收**：先处理输入，再通过 Path 规范化与真实路径判断；读取检查目标 toRealPath，创建检查真实父目录。明确是否允许链接，并考虑校验后替换竞争。测试尾空格、父级段、链接和大小写等平台行为；非法输入应拒绝而非静默重写。

### S04：高副作用工具不受文件权限限制

**位置**：[StoresController.java][stores]，110–154 行；[UnRarService.java][rar]，55–89 行；[SingleDirService.java][single]，55–89 行。

extractRar、singleDir 只检查目录是否存在，不检查 canWriteDir 或 enabled-file-manager。用户可对其他进程可访问目录启动递归任务；解压启用覆盖且成功后删除 RAR，提级移动内容并重命名源目录。@Hidden 仅隐藏文档，不构成权限保护。GET 方法也不适合这些变更操作。

**修复/验收**：默认停用或移出 Web 服务；保留时要求管理员授权、统一写目录校验、POST/CSRF 防护、互斥、预览和备份。关闭总开关、只读目录、目录外输入都应拒绝，且不产生文件副作用。

### S05：密文与主密码同库，服务提供在线解密

**位置**：[application.yml][config]，数据源与 jasypt.encryptor.password；[ToolsController.java][tools]，tools/ymldecrypt；[YmlEncryptorService.java][encrypt]；Redis 测试配置。

仓库同时保存 ENC 密文、主密码和解密算法，Redis 测试配置还含明文凭据。在线解密 API 使用服务端主密码处理客户端输入，叠加 S01 可以匿名调用。密文包装并没有提供仓库泄露后的保密性。本次没有解密或验证任何凭据仍有效。

**修复/验收**：移除线上通用解密 API，把秘密放到环境变量或秘密管理服务；仍有效的凭据应轮换，并检查历史提交、日志和发布产物。只删除当前配置文件不足以处理历史暴露。数据库使用最小权限账户，并按部署条件启用加密传输。

### S06：JWT 默认密钥公开，配置不能控制实际时长

**位置**：TokenValidator.java（历史文件），27 行；LoginService.java（历史文件），34、258–269 行；[ContextConst.java][const]，18 行。

签发和验证读取 login.secret，并提供源码中的默认值；未覆盖时可伪造可通过验签的身份。主配置 auth.jwt.secret 不被这些代码读取，auth.jwt.expire-seconds=86400 也不控制签发。实际常量为 2,592,000 秒，即 30 天。

**修复/验收**：统一配置，缺秘密或使用示例值时拒绝启动；按同一配置读取过期时间，使用稳定用户 ID，验证必要声明。签发后验证实际 exp 差值；默认密钥和错误密钥签发的 Token 应拒绝。即使 S01 修复，本项仍须独立处理。

### S07：管理授权、敏感输出和输入字段隔离缺失

**位置**：[UsersController.java][userscontroller]、[UsersService.java][usersservice]、[UsersDto.java][usersdto]、[UsersEntityMapper.java][usermap]。

用户 CRUD 没有角色、租户和资源归属检查。UsersDto 含 userPassword、mfaKey、inviteToken，映射未排除这些字段，查询可返回对应数据库值。密码是否明文未验证，即使是哈希也不应返回。写入同一 DTO 可设置 isAdmin、tenantId、status 等敏感字段。输入有约束注解，但控制器没有统一以 @Valid 启用验证。

**修复/验收**：拆分查询输出、普通用户编辑和管理员输入 DTO；明确字段白名单及管理员授权。普通用户无法查询/更新其他用户或云资源，响应不包含秘密字段。即使恢复登录检查，也不能把所有登录用户当管理员。文件目录目前同样是全局策略，没有按用户隔离。

### S08：任意 Origin 获得带凭据跨域授权

**位置**：AuthorizationFilter.java（历史文件），89–105 行；[ContextUtils.java][context]，305–312 行。

代码直接回显请求 Origin 并返回 Allow-Credentials=true。Cookie 为 SameSite=None; Secure，在浏览器允许对应 Cookie、HTTPS 目标可达等条件下，不受信任网页可以带凭据跨域读写。第三方 Cookie 限制影响部分浏览器的实际攻击条件，不代表服务端策略安全。高副作用 GET 接口也缺少 CSRF 防护。

**修复/验收**：严格受信任 Origin 列表，按需返回 Vary: Origin；同源应用默认无需跨域。Cookie 会话使用 CSRF 机制，变更接口使用合适方法。已有 HttpOnly/Secure 属性是正确的，不应误报为缺失。用真实浏览器验证可信与不可信来源。

### S09：OAuth 未绑定会话，认证材料进入 URL 和日志

**位置**：[LoginSdk.java][sdk]、LoginController.java（历史文件）、LoginService.java（历史文件）、[MyFeignLogger.java][logger]、[FeignConfig.java][feign]。

GitHub 登录只接收 code，未检查与用户会话绑定的 state，演示页面也未闭环校验。存在登录 CSRF 风险，完整浏览器攻击链未执行。GitHub 官方要求回调 state 与原值一致，否则终止，参见 [OAuth 文档](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps)。

Feign 配置 FULL，自定义 logger 以 INFO 记录请求体、头和响应体，可能记录 client secret、Authorization 和 access token。登录失败分支显式输出 code/Google access token；Google 登录将 Token 放 GET 参数，也可能进入代理访问日志。

**修复/验收**：生成一次性 state，绑定会话并消费；日志统一脱敏、限制 body，凭据避免出现在 URL。测试 state 缺失、错配、重放，以及日志不包含测试秘密。检查历史日志时避免再次扩散原值。

### S10：登出与账号禁用不能撤销访问

**位置**：LoginController.java（历史文件），logout；LoginService.java（历史文件），updateUserInfoAndGenerateToken；TokenValidator.java（历史文件） 与 CookieValidator。

登出仅清 Cookie，无 Token 吊销；保存的 Bearer Token 仍可通过验签。GitHub 登录查到禁用用户后未拒绝签发；验证器只检查 JWT，不查账号状态。getUserInfo 内部的禁用检查仅保护自身接口。

**修复/验收**：明确登出语义，使用会话记录、Token 版本或受控吊销机制；签发时拒绝禁用用户，访问时检查账号状态或短期缓存。覆盖登出后重放、禁用/删除后访问、Token 过期。S01 的失败放行必须先移除才能进行有效访问验收。

## 5. 功能与工程缺陷

### B01：缺少空库初始化路径

[V001][v1] 直接扩展已有 users 表，仓库未发现基础 CREATE TABLE users；[V002][v2] 外键依赖该表。POM 未集成 Flyway，因此迁移目录不会自动执行。

**修复**：提供完整新建库及已有库升级路径，核对外键列类型，验证新库启动及重复部署。不要把当前增量 SQL 描述成完整安装脚本。

### B02：GitHub 配置、回调与邮箱假设不一致

[GithubUtil.java][github] 读取 github.client-id/client-secret，主配置却写 github.oauth.*。仅设置后者的环境变量不足以配置该类。默认 /login/github/callback 没有处理器；演示回调的 /github/callback 只获取第三方资料，不签发本地会话。

LoginService.java（历史文件） 要求邮箱非空，但只请求 GitHub 用户资料，没有专门获取并验证邮箱。未公开邮箱账号可能先插入用户、更新计数，再因邮箱为空失败。Google 登录还未持久化，后续 /login/user/info 可能返回用户不存在。

**修复**：统一配置和入口，以稳定第三方 ID 建立身份，完善邮箱获取和必要验证，明确事务与失败回滚。验证未公开邮箱、重复登录、禁用用户、并发首次登录以及 Google 新用户。不能仅更改 README 就认为登录已修复。

### B03：空目录配置导致空指针

[RealConfigVar.java][dirs] 的 toStoreInfoList 对空字符串返回 null，但 getAllDir 直接 result.addAll(getWriteDir())。启用文件管理时任一目录配置缺失/为空就可能异常，纯只读部署也受影响。

**修复/验收**：统一返回空集合，允许只配 read-dir 或 write-dir。覆盖缺失、空、无效路径和关闭功能。README 暂时要求两项非空，是现状说明而非代码修复。

### B04：readonly 标志错误

[StoreService.java][store] 第 61 行使用 setReadonly(isWritableDir(...))，应取反；getStatus 中已正确取反，同一目录返回矛盾值。

**修复/验收**：统一语义，同一目录 list/status 结果一致。前端上传状态另调 status，所以不能断言所有上传按钮均因此失效；确定问题是列表数据错误。

### B05：Range 协议边界错误与下载错误响应缺失

[StoreService.java][store] 的 downloadWithRange 对 1000 字节文件处理 bytes=-100 时得到 0–100，共 101 字节，而非末尾 100 字节；bytes=0-9999 会 416，正确处理应截断到文件末尾。不支持的 range unit 仍可能返回全文件 206。[RFC 9110 §14.1.2](https://www.rfc-editor.org/rfc/rfc9110.html#section-14.1.2) 给出后缀和超出终点的语义。416 未提供建议的 Content-Range: bytes */长度。

[StoresController.java][stores] 捕获下载异常仅记日志，文件不存在/权限失败可能表现为无明确失败状态的空响应。

**修复/验收**：采用框架支持或完整实现单段 Range，统一下载前错误的 HTTP 状态；验证普通下载、后缀、开放终点、超出末尾、不满足范围、空文件和无权限。已开始写响应后的失败需记录并终止流，不能再返回普通 JSON。

### B06：白名单开关未生效

NoNeedLoginValidator.java（历史文件） 直接遍历 whitelistConfig.getPaths，未检查 enabled，也未调用配置类的检查方法。硬编码正则始终生效，/login、/test 等前缀缺边界限定；/test 已有公开的调试信息接口。未证明这些规则能映射到其他危险现有路由，不将任意后缀绕过当作已验证问题。

**修复**：统一白名单入口，明确内置公开资源和 enabled 语义；验证 enabled=false。customUrl 对可空环境属性直接 env.equals，也需改为安全比较及真实 profile 判断。

### B07：任务竞争、进程控制与跨平台问题

控制器先读 completed，进入异步服务后才置 false，两个请求可能同时启动。[UnRarService.java][rar] 和 [SingleDirService.java][single] 的普通共享状态字段缺少可见性/原子互斥，任务统计可能互相覆盖。

RAR 命令直接拼接密码并记录完整命令。密码空白/选项可造成参数错位；代码没有显式 shell，**未证明通用 shell 命令注入或远程代码执行**。waitFor 无超时，输出无界积累，读取线程未可靠 join。提级用反斜杠拼接路径，不适合 Linux；移动失败无回滚，renameTo 返回值未检查仍计成功。

**修复/验收**：提交前原子获取执行权，按任务 ID 记录状态；使用 ProcessBuilder 参数列表、脱敏、有界输出、超时和取消。路径使用 resolve，定义部分失败和恢复；验证并发启动、冲突文件、含空格密码与跨平台路径。所有验证只用一次性目录。

### B08：审计链不完整

[AuditLogsMapper.java][auditmapper] 以 @Select 声明 DELETE，不适合返回可靠删除行数，应改 @Delete 并验证事务。相关清理控制器已注释，不能声称当前存在开放的删除审计 API。

[AuditLogService.java][auditservice] 内部直接调用自身 @Async 方法，绕过常规 Spring 代理，登录路径并不会按注解意图异步写入。过滤器的访问/失败审计调用被注释。枚举有 ACCESS_SUCCESS，[V002][v2] 的 CHECK 没有该值，恢复调用前需同步约束。

服务无可信代理验证就采信 X-Forwarded-For，审计 IP 可伪造；User-Agent/错误信息没有按列长度截断，超长可能导致写入失败。

**修复/验收**：独立审计写入组件、事件枚举与 DDL 对齐、代理信任策略、长度限制与失败告警。用隔离 MySQL 验证写入、清理行数和失败处理。

### B09：登录类型回填条件不命中旧数据

[V001][v1] 先增加 login_type DEFAULT 1，再 WHERE login_type IS NULL 回填有密码用户。原有记录通常已经得到 1，因此不会被改为密码登录类型。

**修复/验收**：在明确旧记录范围的迁移中先回填，再确定默认值；不能盲目覆盖已经正确使用 OAuth 的数据。验证含密码/无密码的旧记录，以及增量升级重复部署策略。

## 6. 其他观察与待验证事项

- RealConfig 用普通静态 Map 替换配置，缺明确安全发布语义；删除文件保留旧配置，mtime 未递增不刷新。建议不可变快照配合 volatile/原子引用，并明确撤销和失败策略。
- 默认上传上限 10 GB，没有用户配额、并发限制或磁盘预留；大目录全量装载。需容量测试，不能依据源码给出具体性能数字。
- 分页服务创建 Page，但未发现分页拦截器配置；相关扩展入口多被注释，恢复前验证实际 SQL 的 LIMIT 和总数。
- getRemainingTtl 将 JWT exp 直接转 Date，可能存在 NumericDate 秒与毫秒解释差异；没有发现调用点，列为待验证，不作为确定的线上故障。
- 前端隐藏删除逻辑请求不存在的 /uploadFile/deleteFile，且使用 StoreInfo 没有的 key；属于残留功能。common.js 固定 API 根路径 /，子路径部署需适配。
- 用户删除用 removeById，实体 delflag 没有 @TableLogic；当前配置也未见对应全局逻辑删除配置，应明确硬删除与软删除语义。
- 静态第三方 JS 缺统一依赖清单和升级流程。建议 SBOM、依赖审计及许可证清单；本次未完成 CVE 匹配。
- 测试集中在枚举、IP、Redis，缺文件权限和认证回归。部分用 JUnit 4 注解，需确认引擎发现；Redis 配置指向原环境，测试写固定键，不能直接用于 CI。

## 7. 验证记录与限制

| 检查 | 结果 | 边界 |
| --- | --- | --- |
| 基线/工作区 | HEAD 318525b，保留原有两项配置删除 | 未修改业务代码 |
| 工具链 | Java 21.0.3、Maven 3.9.9 可用 | 不代表成功编译 |
| mvn -o -DskipTests package | 失败：本地 Maven 镜像未缓存 Spring Boot 3.4.5 父 POM | 模型解析阶段终止，不能归因为源码编译错误 |
| 上传目标路径计算 | normalize 后位于允许根之外 | 只计算路径，未写业务文件 |
| 提取实际 clearFileName 执行 | 尾部空格清理后残留 .. | 实际方法验证，未启动 Web |
| Range 解析片段 | -100 得到 0–100 而非 900–999 | 逻辑片段，不是 HTTP 集成测试 |
| JWT 常量计算 | 2,592,000 秒 | 结合签发调用链，不是 OAuth 联调 |
| 完整测试与启动、第三方集成 | 未执行 | 依赖解析阻塞，未使用外部业务服务 |

独立 Java 验证仅在临时目录写入测试源码，运行结果：

```text
UPLOAD_FILENAME_ESCAPES_ROOT=true (path-only; no file written)
SUFFIX_RANGE_ACTUAL=0-100 EXPECTED=900-999 for a 1000-byte file
TRIM_AFTER_SANITIZE_LEAVES_DOTDOT=true (actual extracted method)
JWT_LIFETIME_SECONDS=2592000
```

构建问题属于当前 Maven 环境限制。后续应在可访问的 Maven 仓库、独立数据库和文件沙箱中验证；不能把未执行的测试标记为通过。静态确定的控制流问题不因完整构建受阻而消失。

## 8. 建议修复顺序与交付边界

1. **先关闭暴露面**：默认拒绝未认证请求；停用文件整理和在线解密入口；隔离云凭据，核对仍有效秘密是否需轮换。
2. **统一权限与文件边界**：角色/资源授权、最终目标路径校验、原子创建和链接策略；越界请求必须失败且无文件副作用。
3. **完善身份生命周期**：统一 OAuth 配置与回调，state、稳定 ID、禁用检查、登出吊销和日志脱敏。
4. **实现可重复安装**：空库初始化、增量迁移、无秘密的配置、可选云模块开关；全新环境验证 README 命令。
5. **补正确性与 CI**：Range、空目录、readonly、任务互斥和审计一致性；核对真实用例数，避免零测试通过。

本次交付为 README 和本报告。报告中的问题仍待代码修复；文档更新不代表安全整改完成。

[store]: ../beinet-deployment/beinet-dp-admin/src/main/java/cn/beinet/deployment/admin/stores/services/StoreService.java
[stores]: ../beinet-deployment/beinet-dp-admin/src/main/java/cn/beinet/deployment/admin/stores/StoresController.java
[file]: ../beinet-core/beinet-utils/src/main/java/cn/beinet/core/utils/FileHelper.java
[dirs]: ../beinet-deployment/beinet-dp-admin/src/main/java/cn/beinet/deployment/admin/stores/configs/RealConfigVar.java
[rar]: ../beinet-deployment/beinet-dp-admin/src/main/java/cn/beinet/deployment/admin/stores/services/UnRarService.java
[single]: ../beinet-deployment/beinet-dp-admin/src/main/java/cn/beinet/deployment/admin/stores/services/SingleDirService.java
[config]: ../beinet-deployment/beinet-dp-admin/src/main/resources/application.yml
[tools]: ../beinet-deployment/beinet-dp-admin/src/main/java/cn/beinet/deployment/admin/tools/ToolsController.java
[encrypt]: ../beinet-deployment/beinet-dp-admin/src/main/java/cn/beinet/deployment/admin/tools/service/YmlEncryptorService.java
[const]: ../beinet-core/beinet-base/src/main/java/cn/beinet/core/base/consts/ContextConst.java
[userscontroller]: ../beinet-deployment/beinet-dp-admin/src/main/java/cn/beinet/deployment/admin/users/controller/UsersController.java
[usersservice]: ../beinet-business/beinet-login/src/main/java/cn/beinet/business/login/service/UsersService.java
[usersdto]: ../beinet-business/beinet-login/src/main/java/cn/beinet/business/login/dto/UsersDto.java
[usermap]: ../beinet-business/beinet-login/src/main/java/cn/beinet/business/login/mapstruct/UsersEntityMapper.java
[context]: ../beinet-core/beinet-web/src/main/java/cn/beinet/core/web/context/ContextUtils.java
[sdk]: ../beinet-sdk/beinet-login-sdk/src/main/java/cn/beinet/sdk/login/LoginSdk.java
[logger]: ../beinet-core/beinet-feign/src/main/java/cn/beinet/core/feign/log/MyFeignLogger.java
[feign]: ../beinet-core/beinet-feign/src/main/java/cn/beinet/core/feign/FeignConfig.java
[v1]: ../beinet-deployment/beinet-dp-admin/src/main/resources/db/migration/V001__extend_users_table_for_github_oauth.sql
[v2]: ../beinet-deployment/beinet-dp-admin/src/main/resources/db/migration/V002__create_audit_logs_table.sql
[github]: ../beinet-core/beinet-third/src/main/java/cn/beinet/core/thirdparty/github/GithubUtil.java
[auditmapper]: ../beinet-business/beinet-login/src/main/java/cn/beinet/business/login/dal/AuditLogsMapper.java
[auditservice]: ../beinet-business/beinet-login/src/main/java/cn/beinet/business/login/service/AuditLogService.java
