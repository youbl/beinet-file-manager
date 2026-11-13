# Google / GitHub 登录与邮箱准入

更新日期：2026-10-21。当前实现为普通MVC Controller + HttpSession + AuthorizationFilter，已删除SecurityConfiguration及Spring Security依赖。

## 登录与访问控制

1. 登录页通过/auth/providers获取已配置供应商；两项凭据全空隐藏，只填一项启动失败。
2. AuthController生成随机state与PKCE verifier，Session保存五分钟，浏览器携带S256 challenge进入供应商。
3. 回调先校验并一次消费state和供应商，再由OAuthGateway用固定HTTPS端点换票、读取用户身份。
4. Google使用OAuth2用户信息接口的id/email/verified_email；不使用OIDC ID Token或浏览器JWT。GitHub读取用户和已验证邮箱列表。
5. 邮箱精确匹配allowed-emails与admin-emails并集，忽略大小写和首尾空格；管理员不必重复填写。两个列表都空则拒绝所有人。
6. 以provider+subject事务创建/更新本地身份；禁用或软删除拒绝，不按邮箱合并。
7. 登录成功销毁旧Session、新建Session保存LoginUser，生成新CSRF，安全回跳。Token仅在回调内临时使用。
8. AuthorizationFilter逐项检查公开路径、Session登录、名单、写请求CSRF/Origin、账号状态、停用路径和管理员权限，最后设置可信请求属性。

## 路径与会话

必要登录入口、回调、favicon及/res资源仅GET/HEAD公开。业务HTML需要登录，匿名API返回401、HTML跳登录；管理权限不足或CSRF无效403，查账号数据库异常503。会话失效或名单/账号撤销后不可继续访问。

CSRF随机值保存在Session，前端通过XSRF-TOKEN Cookie取得并使用X-XSRF-TOKEN Header回传。POST退出也经过账号状态及CSRF验证，随后销毁Session并清Cookie。认证Cookie为HttpOnly、SameSite=Lax；生产HTTPS使用Secure，本机HTTP显式关闭。会话默认8小时闲置过期，重启失效。

过滤器为普通Servlet Filter，不存在SecurityContextHolder或框架角色。不得重复注册。路径歧义输入返回400，TRACE/CONNECT返回405。日志只记录必要身份及异常链，不输出认证材料。

## 文档与验收

[需求文档](requirements/oauth-access-control/REQUIREMENTS.md)、[技术文档及全部内嵌UML](requirements/oauth-access-control/TECHNICAL_DESIGN.md)、[完整部署指南](DEPLOYMENT.md)、[一次性建库SQL](sql/01-init-database.sql)。

旧Spring版本测试数量属于历史记录，当前版本使用AuthorizationFilterTest、OAuthGatewayTest等重新验证。真实Google/GitHub、代理证书及生产数据库仍需目标环境验收。Google既有身份标识对应关系需要用原账号联调核对，不能自动按邮箱迁移。

## 登录诊断

成功日志包含排查编号、供应商、本地和供应商用户ID、邮箱姓名、准入/管理员判断、客户端地址、User-Agent、回调和origin。失败日志包含同一排查编号、受控reason、已知身份上下文、异常链和SQL错误码/HTTP状态。页面给出具体原因及编号，原始异常消息和供应商响应不传给页面或日志。

## 未完成范围

文件路径与Range等历史风险需另行修复；audit_logs表存在不代表新认证已全面持久化审计；多实例共享Session未实现；健康检查仍查用户计数，连接获取和SQL分段计时及探针拆分未实现。


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
