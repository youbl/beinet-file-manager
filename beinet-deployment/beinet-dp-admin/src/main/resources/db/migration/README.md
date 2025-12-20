# 数据库迁移说明

## 迁移脚本执行顺序

1. **V001__extend_users_table_for_github_oauth.sql** - 扩展用户表支持 GitHub OAuth
2. **V002__create_audit_logs_table.sql** - 创建审计日志表

## 手动执行方式

如果使用 Flyway 或类似工具，脚本会自动执行。如果需要手动执行：

```sql
-- 1. 连接到数据库
USE eb-dev;

-- 2. 执行用户表扩展脚本
SOURCE V001__extend_users_table_for_github_oauth.sql;

-- 3. 执行审计日志表创建脚本
SOURCE V002__create_audit_logs_table.sql;

-- 4. 验证表结构
DESCRIBE users;
DESCRIBE audit_logs;

-- 5. 检查索引
SHOW INDEX FROM users;
SHOW INDEX FROM audit_logs;
```

## 回滚方式

如果需要回滚更改：

```sql
-- 回滚审计日志表
DROP TABLE IF EXISTS audit_logs;

-- 回滚用户表扩展
ALTER TABLE users DROP COLUMN IF EXISTS github_id;
ALTER TABLE users DROP COLUMN IF EXISTS github_login;
ALTER TABLE users DROP COLUMN IF EXISTS github_avatar_url;
ALTER TABLE users DROP COLUMN IF EXISTS github_node_id;
ALTER TABLE users DROP COLUMN IF EXISTS login_type;
ALTER TABLE users DROP COLUMN IF EXISTS last_login_ip;
ALTER TABLE users DROP COLUMN IF EXISTS login_count;

-- 删除索引
DROP INDEX IF EXISTS idx_users_github_id ON users;
DROP INDEX IF EXISTS idx_users_github_login ON users;
DROP INDEX IF EXISTS idx_users_login_type ON users;
DROP INDEX IF EXISTS idx_users_last_login ON users;
DROP INDEX IF EXISTS idx_users_status_type ON users;
```

## 注意事项

1. 执行前请备份数据库
2. 在生产环境执行前，请先在测试环境验证
3. 确保应用程序停止后再执行迁移
4. 执行后验证数据完整性