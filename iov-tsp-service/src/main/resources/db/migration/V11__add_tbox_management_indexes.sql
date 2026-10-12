-- V11__add_tbox_management_indexes.sql
-- TSP-DSN-CR-009: TBOX 后台运营查询（MPT）索引补强
-- 仅补充缺失索引，不新增业务表；已有等价索引不重复创建。
-- MySQL 8.0 不支持 CREATE INDEX IF NOT EXISTS，使用存储过程守卫幂等。

DROP PROCEDURE IF EXISTS tsp_add_tbox_management_indexes;
DELIMITER $$
CREATE PROCEDURE tsp_add_tbox_management_indexes()
BEGIN
    -- 1. tb_tbox(device_status)：按设备状态筛选/排序
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'tb_tbox' AND index_name = 'idx_tbox_device_status') THEN
        CREATE INDEX idx_tbox_device_status ON tb_tbox(device_status);
    END IF;

    -- 2. tb_tbox(first_login_time)：首次登录时间范围筛选
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'tb_tbox' AND index_name = 'idx_tbox_first_login_time') THEN
        CREATE INDEX idx_tbox_first_login_time ON tb_tbox(first_login_time);
    END IF;

    -- 3. tb_tbox(last_login_time)：长期未登录识别（V9 可能已建）
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'tb_tbox' AND index_name = 'idx_tbox_last_login_time') THEN
        CREATE INDEX idx_tbox_last_login_time ON tb_tbox(last_login_time);
    END IF;

    -- 4. tb_tbox(activate_time)：激活时间范围筛选
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'tb_tbox' AND index_name = 'idx_tbox_activate_time') THEN
        CREATE INDEX idx_tbox_activate_time ON tb_tbox(activate_time);
    END IF;

    -- 5. tb_vehicle_tbox(sn, device_category, bind_state)：按 TBOX grain 聚合 active 绑定
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'tb_vehicle_tbox' AND index_name = 'idx_vehicle_tbox_sn_state') THEN
        CREATE INDEX idx_vehicle_tbox_sn_state ON tb_vehicle_tbox(sn, device_category, bind_state);
    END IF;

    -- 6. tb_vehicle_tbox(vin, device_category, bind_state)：按 VIN 过滤/接入身份反查
    --    V8 已建等价索引 idx_vehicle_tbox_vin_active，存在时不再重复创建
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'tb_vehicle_tbox' AND index_name = 'idx_vehicle_tbox_vin_active') THEN
        IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                       WHERE table_schema = DATABASE() AND table_name = 'tb_vehicle_tbox' AND index_name = 'idx_vehicle_tbox_vin_state') THEN
            CREATE INDEX idx_vehicle_tbox_vin_state ON tb_vehicle_tbox(vin, device_category, bind_state);
        END IF;
    END IF;

    -- 7. tb_tbox_registration_event(device_sn, event_type, occurred_at)：时间线按设备读取消费幂等记录
    IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                   WHERE table_schema = DATABASE() AND table_name = 'tb_tbox_registration_event' AND index_name = 'idx_reg_event_device_type_time') THEN
        CREATE INDEX idx_reg_event_device_type_time ON tb_tbox_registration_event(device_sn, event_type, occurred_at);
    END IF;
END$$
DELIMITER ;

CALL tsp_add_tbox_management_indexes();
DROP PROCEDURE tsp_add_tbox_management_indexes;
