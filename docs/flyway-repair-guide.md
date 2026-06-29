# Flyway 数据库迁移修复指南

## 问题描述

数据库中 `flyway_schema_history` 表包含旧脚本名称的记录（V2, V3, V20260627, V20260628），但迁移脚本已重命名为新的命名规范（V1, V2, V3, V4）。这会导致 Flyway 启动失败。

## 解决方案

### 方案1：使用 Maven Flyway 插件修复（推荐）

```bash
cd iov-tsp-service

# 修复迁移历史表
mvn flyway:repair

# 验证迁移状态
mvn flyway:info
```

### 方案2：手动执行 SQL 修复脚本

如果 Maven 插件修复失败，可以手动执行 SQL 脚本：

1. 连接到 MySQL 数据库
2. 执行 `src/main/resources/db/migration/repair_flyway_history.sql`

### 方案3：核选项（慎用）

如果以上方案都失败，可以清空历史表：

```sql
TRUNCATE TABLE flyway_schema_history;
```

**警告：** 这会导致所有迁移重新执行，可能造成数据丢失！

## 迁移脚本命名规范

| 版本 | 脚本名 | 说明 |
|------|--------|------|
| V1 | V1__drop_idcm_tables.sql | 删除 IDCM 相关表 |
| V2 | V2__add_activate_time_to_tb_tbox.sql | 添加 activate_time 字段 |
| V3 | V3__add_device_admission_support.sql | 设备接入鉴权支持 |
| V4 | V4__add_binding_projection_support.sql | 绑定投影支持 |

## 验证步骤

1. 执行修复后，检查迁移历史：
   ```sql
   SELECT * FROM flyway_schema_history ORDER BY installed_rank;
   ```

2. 启动应用，确认 Flyway 不再报错

3. 检查数据库表结构是否完整
