-- V15__audit_owner_utc.sql
-- TSP-DSN-CR-014：普通审计单属主（modify_time 由 DB ON UPDATE 唯一生成）。
-- 配套代码变更：全部 XML insert/update/logicalDelete 已停止显式 now() 写入，
--   insert 依赖 DEFAULT CURRENT_TIMESTAMP，update/delete 依赖 ON UPDATE CURRENT_TIMESTAMP。
-- 边界：
--   * 业务时间列（activate_time / first_login_time / last_login_time / occurred_at /
--     processed_at / last_event_time / checked_at / msg_time / msg_ack_time 等）不套 ON UPDATE，
--     保留原业务属主（事件来源 / set-once / max / ACK / decisionAt）。
--   * tb_vehicle_sim / tb_vehicle_sim_log / tb_device_admission_log 已具备 ON UPDATE（V3/V5），不重复变更。
--   * tb_tbox_registration_event.create_time 为 App 唯一写者（Inbox 事务），保持现状，不套 ON UPDATE。
--   * 精度保持现有秒级（无 (n)）；DATETIME(3) 目标属逐表 T3 治理，本批不修改存量列精度。
-- MySQL 8.0 不支持 MODIFY COLUMN IF EXISTS，使用存储过程守卫幂等（对齐既有 V8/V9/V12 模式）。

DROP PROCEDURE IF EXISTS tsp_add_audit_on_update;
DELIMITER $$
CREATE PROCEDURE tsp_add_audit_on_update()
BEGIN
    DECLARE has_on_update INT DEFAULT 0;

    -- tb_tbox
    SELECT COUNT(*) INTO has_on_update
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_tbox' AND COLUMN_NAME = 'modify_time'
      AND EXTRA LIKE '%on update CURRENT_TIMESTAMP%';
    IF has_on_update = 0 THEN
        ALTER TABLE tb_tbox MODIFY COLUMN modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间';
    END IF;

    -- tb_ccp
    SELECT COUNT(*) INTO has_on_update
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_ccp' AND COLUMN_NAME = 'modify_time'
      AND EXTRA LIKE '%on update CURRENT_TIMESTAMP%';
    IF has_on_update = 0 THEN
        ALTER TABLE tb_ccp MODIFY COLUMN modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间';
    END IF;

    -- tb_vehicle_tbox
    SELECT COUNT(*) INTO has_on_update
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_vehicle_tbox' AND COLUMN_NAME = 'modify_time'
      AND EXTRA LIKE '%on update CURRENT_TIMESTAMP%';
    IF has_on_update = 0 THEN
        ALTER TABLE tb_vehicle_tbox MODIFY COLUMN modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间';
    END IF;

    -- tb_vehicle_ccp
    SELECT COUNT(*) INTO has_on_update
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_vehicle_ccp' AND COLUMN_NAME = 'modify_time'
      AND EXTRA LIKE '%on update CURRENT_TIMESTAMP%';
    IF has_on_update = 0 THEN
        ALTER TABLE tb_vehicle_ccp MODIFY COLUMN modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间';
    END IF;

    -- tb_cmd_record
    SELECT COUNT(*) INTO has_on_update
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_cmd_record' AND COLUMN_NAME = 'modify_time'
      AND EXTRA LIKE '%on update CURRENT_TIMESTAMP%';
    IF has_on_update = 0 THEN
        ALTER TABLE tb_cmd_record MODIFY COLUMN modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间';
    END IF;

    -- tb_tbox_log
    SELECT COUNT(*) INTO has_on_update
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_tbox_log' AND COLUMN_NAME = 'modify_time'
      AND EXTRA LIKE '%on update CURRENT_TIMESTAMP%';
    IF has_on_update = 0 THEN
        ALTER TABLE tb_tbox_log MODIFY COLUMN modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间';
    END IF;

    -- tb_ccp_log
    SELECT COUNT(*) INTO has_on_update
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_ccp_log' AND COLUMN_NAME = 'modify_time'
      AND EXTRA LIKE '%on update CURRENT_TIMESTAMP%';
    IF has_on_update = 0 THEN
        ALTER TABLE tb_ccp_log MODIFY COLUMN modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间';
    END IF;

    -- tb_vehicle_tbox_log
    SELECT COUNT(*) INTO has_on_update
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_vehicle_tbox_log' AND COLUMN_NAME = 'modify_time'
      AND EXTRA LIKE '%on update CURRENT_TIMESTAMP%';
    IF has_on_update = 0 THEN
        ALTER TABLE tb_vehicle_tbox_log MODIFY COLUMN modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间';
    END IF;

    -- tb_vehicle_ccp_log
    SELECT COUNT(*) INTO has_on_update
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'tb_vehicle_ccp_log' AND COLUMN_NAME = 'modify_time'
      AND EXTRA LIKE '%on update CURRENT_TIMESTAMP%';
    IF has_on_update = 0 THEN
        ALTER TABLE tb_vehicle_ccp_log MODIFY COLUMN modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间';
    END IF;
END$$
DELIMITER ;

CALL tsp_add_audit_on_update();
DROP PROCEDURE tsp_add_audit_on_update;
