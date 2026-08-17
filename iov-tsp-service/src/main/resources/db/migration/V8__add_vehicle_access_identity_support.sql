-- V8__add_vehicle_access_identity_support.sql
-- TSP-DSN-CR-007: 车辆接入身份反查（VIN → ACTIVE TBOX 绑定投影 → tb_tbox.hsm）
-- 仅补充缺失索引，不新增业务表

-- MySQL 8.0 不支持 CREATE INDEX IF NOT EXISTS，使用存储过程守卫幂等
DROP PROCEDURE IF EXISTS tsp_add_vehicle_access_identity_indexes;
DELIMITER $$
CREATE PROCEDURE tsp_add_vehicle_access_identity_indexes()
BEGIN
    -- 1. tb_vehicle_tbox(vin, device_category, bind_state)：接入身份反查按 VIN 查 ACTIVE TBOX 绑定
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'tb_vehicle_tbox' AND index_name = 'idx_vehicle_tbox_vin_active') THEN
        CREATE INDEX idx_vehicle_tbox_vin_active ON tb_vehicle_tbox(vin, device_category, bind_state);
    END IF;

    -- 2. tb_tbox(sn)：接入身份反查按投影 sn 关联设备信息（基表 DDL 应已含唯一索引，缺失时补充）
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'tb_tbox' AND index_name = 'uk_tbox_sn') THEN
        CREATE UNIQUE INDEX uk_tbox_sn ON tb_tbox(sn);
    END IF;

    -- 3. tb_tbox(hsm)：确认已存在（V3 已建 idx_hsm），缺失时补充
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'tb_tbox' AND index_name = 'idx_hsm') THEN
        CREATE INDEX idx_hsm ON tb_tbox(hsm);
    END IF;
END$$
DELIMITER ;

CALL tsp_add_vehicle_access_identity_indexes();
DROP PROCEDURE tsp_add_vehicle_access_identity_indexes;
