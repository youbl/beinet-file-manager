# 数据库迁移

脚本手动执行，项目没有集成 Flyway。先备份，在目标 MySQL 版本的隔离副本演练；正式迁移期间停止应用写入。

## 空数据库

依次执行：

1. ../bootstrap.sql
2. V001__extend_users_table_for_github_oauth.sql
3. V002__create_audit_logs_table.sql
4. V003__oauth_identities.sql

bootstrap.sql 仅用于空库，不要对已有库执行。

## 已有数据库

已执行 V001/V002 的环境只执行 V003。更老的环境先核对列和索引，执行尚未应用的脚本。所有脚本均不是可随意重跑的幂等脚本。

V003 创建 oauth_identities，并将 users.github_id 迁移为 github 身份，包含禁用/删除用户，避免重新登录绕过原账号状态。执行前确认 users.id 为兼容的 BIGINT 类型，表使用 InnoDB，github_id 无重复；特殊旧表结构需要先适配外键类型。

执行后检查身份表条数与迁移前非空 github_id 记录数一致、无孤立 user_id，并验证禁用用户仍不能登录。Google 新用户会在首次通过白名单验证后创建独立身份，不自动按邮箱合并旧记录。

## 回滚

恢复迁移前的数据库备份和匹配的应用版本；不要直接删表再继续运行新代码。oauth_identities 保存 Google 和新建第三方身份映射，删除它会丢失关联。备份方案需要包含 users、oauth_identities 和 audit_logs 的一致快照。

本地测试使用 H2 MySQL 模式执行完整脚本并测试身份操作；这不能代替真实 MySQL 迁移验证。
