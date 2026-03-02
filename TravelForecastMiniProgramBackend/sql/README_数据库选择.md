# 数据库选择说明

## 📋 两种方案

### 方案一：使用现有数据库 `travel_prediction`（推荐）

**优点**：
- ✅ 与 Web 后端共享数据库，符合架构设计
- ✅ 数据统一管理
- ✅ 不需要新建数据库

**步骤**：
1. 修改 `application.yml` 中的数据库名称为 `travel_prediction`（已自动修改）
2. 执行 `init_mp_tables_in_travel_prediction.sql` 创建小程序专用表

### 方案二：使用新数据库 `travel_forecast`

**优点**：
- ✅ 数据库隔离，更清晰
- ✅ 不影响现有系统

**步骤**：
1. 执行 `00_create_database.sql` 创建数据库
2. 执行 `01_init_all_tables.sql` 创建所有表
3. 保持 `application.yml` 中的数据库名称为 `travel_forecast`

---

## 🎯 推荐方案

**建议使用方案一**（`travel_prediction`），因为：
1. 符合微服务架构文档的设计（共享数据库，表前缀区分）
2. Web 后端和小程序后端可以共享 `users` 表等基础数据
3. 数据一致性更好

---

## ✅ 当前配置

已自动将 `application.yml` 配置为使用 `travel_prediction` 数据库。

请执行以下 SQL 脚本创建小程序专用表：
```
sql/init_mp_tables_in_travel_prediction.sql
```
