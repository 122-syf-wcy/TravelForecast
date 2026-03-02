# 数据库初始化说明

## 📋 执行顺序

### 方式一：使用 Navicat / DBeaver 等工具

1. **创建数据库**
   - 打开 `00_create_database.sql`
   - 执行整个脚本（会创建 `travel_forecast` 数据库）

2. **创建表结构**
   - 打开 `01_init_all_tables.sql`
   - 执行整个脚本（会创建所有需要的表）

### 方式二：使用 MySQL 命令行

```bash
# 1. 登录 MySQL
mysql -u root -p

# 2. 执行创建数据库脚本
source F:/Web/TravelForecastMiniProgramBackend/sql/00_create_database.sql

# 3. 执行创建表脚本
source F:/Web/TravelForecastMiniProgramBackend/sql/01_init_all_tables.sql
```

### 方式三：使用命令行（一行命令）

```bash
mysql -u root -p < F:/Web/TravelForecastMiniProgramBackend/sql/00_create_database.sql
mysql -u root -p travel_forecast < F:/Web/TravelForecastMiniProgramBackend/sql/01_init_all_tables.sql
```

---

## ⚠️ 注意事项

1. **如果数据库已存在**
   - `00_create_database.sql` 使用 `CREATE DATABASE IF NOT EXISTS`，不会报错
   - 如果数据库已存在且包含数据，请谨慎执行

2. **如果表已存在**
   - `users` 表使用 `CREATE TABLE IF NOT EXISTS`，不会覆盖现有数据
   - `mp_wechat_users` 表也使用 `CREATE TABLE IF NOT EXISTS`，不会覆盖现有数据

3. **数据库名称**
   - 默认数据库名：`travel_forecast`
   - 如需修改，请同时修改：
    - SQL 脚本中的数据库名
    - `application.yml` 中的 `spring.datasource.url`

---

## ✅ 验证数据库创建成功

执行以下 SQL 验证：

```sql
USE travel_forecast;

-- 查看所有表
SHOW TABLES;

-- 应该看到：
-- - users
-- - mp_wechat_users

-- 查看表结构
DESC users;
DESC mp_wechat_users;
```

---

## 🔧 如果遇到问题

### 问题1：数据库已存在但表不存在
直接执行 `01_init_all_tables.sql` 即可。

### 问题2：表已存在但结构不同
- 备份现有数据
- 删除旧表：`DROP TABLE IF EXISTS mp_wechat_users;`
- 重新执行 `01_init_all_tables.sql`

### 问题3：权限不足
确保 MySQL 用户有 `CREATE DATABASE` 和 `CREATE TABLE` 权限。

---

**最后更新**: 2026-02-07
