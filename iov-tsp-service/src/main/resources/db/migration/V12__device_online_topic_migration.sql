-- V12__device_online_topic_migration.sql
-- TSP-DSN-CR-010：DEVICE_ONLINE 切换至 vagw.device.online 的数据侧支撑
-- 1) tb_tbox.hsm 规范化（trim+uppercase）
-- 2) 规范化重复预检 fail-fast（禁止自动合并）
-- 3) 生成列 active_hsm（仅 row_valid=1 参与唯一约束；软删映射 NULL，可重新导入相同 hsm）
-- 4) 删除等价非唯一索引 idx_hsm
-- 5) tb_tbox_registration_event 增加可空 source_topic / connection_generation
-- MySQL 8.0 不支持 ADD COLUMN / CREATE INDEX IF NOT EXISTS，使用存储过程守卫幂等。

-- 1) 存量规范化
UPDATE tb_tbox
SET hsm = UPPER(TRIM(hsm))
WHERE hsm IS NOT NULL
  AND hsm <> UPPER(TRIM(hsm));

-- 2) 规范化重复预检（仅有效行、非空 hsm）
DROP PROCEDURE IF EXISTS tsp_check_tbox_hsm_duplicate;
DELIMITER $$
CREATE PROCEDURE tsp_check_tbox_hsm_duplicate()
BEGIN
    DECLARE dup_groups INT DEFAULT 0;
    SELECT COUNT(*) INTO dup_groups FROM (
        SELECT hsm
        FROM tb_tbox
        WHERE row_valid = 1 AND hsm IS NOT NULL AND hsm <> ''
        GROUP BY hsm
        HAVING COUNT(*) > 1
    ) d;
    IF dup_groups > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'TSP-DSN-CR-010: tb_tbox.hsm 规范化后存在重复，须人工清理后再迁移（禁止自动合并）';
    END IF;
END$$
DELIMITER ;

CALL tsp_check_tbox_hsm_duplicate();
DROP PROCEDURE tsp_check_tbox_hsm_duplicate;

-- 3)+4) 生成列唯一约束与旧索引清理
DROP PROCEDURE IF EXISTS tsp_add_tbox_active_hsm;
DELIMITER $$
CREATE PROCEDURE tsp_add_tbox_active_hsm()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'tb_tbox' AND column_name = 'active_hsm') THEN
        ALTER TABLE tb_tbox ADD COLUMN active_hsm VARCHAR(100)
            GENERATED ALWAYS AS (CASE WHEN row_valid = 1 THEN UPPER(TRIM(hsm)) ELSE NULL END) STORED
            COMMENT '仅有效行的规范化 HSM（唯一约束载体，软删为 NULL）';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'tb_tbox' AND index_name = 'uk_tbox_active_hsm') THEN
        CREATE UNIQUE INDEX uk_tbox_active_hsm ON tb_tbox(active_hsm);
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.statistics
               WHERE table_schema = DATABASE() AND table_name = 'tb_tbox' AND index_name = 'idx_hsm') THEN
        DROP INDEX idx_hsm ON tb_tbox;
    END IF;
END$$
DELIMITER ;

CALL tsp_add_tbox_active_hsm();
DROP PROCEDURE tsp_add_tbox_active_hsm;

-- 5) 事件事实表新增可空列
DROP PROCEDURE IF EXISTS tsp_add_reg_event_device_online_columns;
DELIMITER $$
CREATE PROCEDURE tsp_add_reg_event_device_online_columns()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'tb_tbox_registration_event' AND column_name = 'source_topic') THEN
        ALTER TABLE tb_tbox_registration_event ADD COLUMN source_topic VARCHAR(128) NULL
            COMMENT '来源 Kafka Topic（DEVICE_ONLINE）';
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'tb_tbox_registration_event' AND column_name = 'connection_generation') THEN
        ALTER TABLE tb_tbox_registration_event ADD COLUMN connection_generation BIGINT NULL
            COMMENT 'VAGW 连接代际（DEVICE_ONLINE）';
    END IF;
END$$
DELIMITER ;

CALL tsp_add_reg_event_device_online_columns();
DROP PROCEDURE tsp_add_reg_event_device_online_columns;
