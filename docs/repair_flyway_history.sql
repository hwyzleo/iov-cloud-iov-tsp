-- repair_flyway_history.sql
-- 手动修复 flyway_schema_history 表，更新脚本名称以匹配新的命名规范
-- 使用前请先备份数据库！

-- 查看当前历史记录
SELECT * FROM flyway_schema_history ORDER BY installed_rank;

-- 方案A：更新版本号和脚本名称（推荐）
-- 将旧的脚本名称更新为新的命名规范

-- 更新 V2 -> V1 (drop_idcm_tables)
UPDATE flyway_schema_history 
SET version = '1', script = 'V1__drop_idcm_tables.sql' 
WHERE script = 'V2__drop_idcm_tables.sql';

-- 更新 V3 -> V2 (add_activate_time)
UPDATE flyway_schema_history 
SET version = '2', script = 'V2__add_activate_time_to_tb_tbox.sql' 
WHERE script = 'V3__add_activate_time_to_tb_tbox.sql';

-- 更新 V20260627 -> V3 (device_admission)
UPDATE flyway_schema_history 
SET version = '3', script = 'V3__add_device_admission_support.sql' 
WHERE script = 'V20260627__add_device_admission_support.sql';

-- 更新 V20260628 -> V4 (binding_projection)
UPDATE flyway_schema_history 
SET version = '4', script = 'V4__add_binding_projection_support.sql' 
WHERE script = 'V20260628__add_binding_projection_support.sql';

-- 更新 V1_0_002 -> V2 (如果存在)
UPDATE flyway_schema_history 
SET version = '2', script = 'V2__add_activate_time_to_tb_tbox.sql' 
WHERE script = 'V1_0_002__add_activate_time_to_tb_tbox.sql';

-- 更新 V20260629 -> V2 (如果存在)
UPDATE flyway_schema_history 
SET version = '2', script = 'V2__add_activate_time_to_tb_tbox.sql' 
WHERE script = 'V20260629__add_activate_time_to_tb_tbox.sql';

-- 更新 checksum 为 NULL 以避免校验和不匹配
UPDATE flyway_schema_history SET checksum = NULL WHERE script IN (
    'V1__drop_idcm_tables.sql',
    'V2__add_activate_time_to_tb_tbox.sql',
    'V3__add_device_admission_support.sql',
    'V4__add_binding_projection_support.sql'
);

-- 方案B：核选项 - 清空历史表（慎用！可能导致迁移重新执行）
-- TRUNCATE TABLE flyway_schema_history;

-- 验证更新结果
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
