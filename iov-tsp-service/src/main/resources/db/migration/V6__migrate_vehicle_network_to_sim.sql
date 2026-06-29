-- V6__migrate_vehicle_network_to_sim.sql
-- 将 tb_vehicle_network 数据迁移到 tb_vehicle_sim
-- 按 iccid1 (slot 1) / iccid2 (slot 2) 拆为子表行

-- 1. 迁移 iccid1 (slot 1)
INSERT INTO tb_vehicle_sim (vin, slot_no, iccid, package_code, description, create_by, create_time, modify_by, modify_time, row_version, row_valid)
SELECT
    vin,
    1 AS slot_no,
    iccid1 AS iccid,
    package_code,
    description,
    create_by,
    create_time,
    modify_by,
    modify_time,
    row_version,
    row_valid
FROM tb_vehicle_network
WHERE iccid1 IS NOT NULL AND iccid1 != ''
AND row_valid = 1;

-- 2. 迁移 iccid2 (slot 2)
INSERT INTO tb_vehicle_sim (vin, slot_no, iccid, package_code, description, create_by, create_time, modify_by, modify_time, row_version, row_valid)
SELECT
    vin,
    2 AS slot_no,
    iccid2 AS iccid,
    package_code,
    description,
    create_by,
    create_time,
    modify_by,
    modify_time,
    row_version,
    row_valid
FROM tb_vehicle_network
WHERE iccid2 IS NOT NULL AND iccid2 != ''
AND row_valid = 1;

-- 3. 迁移日志表数据 - iccid1 (slot 1)
INSERT INTO tb_vehicle_sim_log (vin, slot_no, iccid, package_code, description, create_by, create_time, modify_by, modify_time, row_version, row_valid)
SELECT
    vin,
    1 AS slot_no,
    iccid1 AS iccid,
    package_code,
    description,
    create_by,
    create_time,
    modify_by,
    modify_time,
    row_version,
    row_valid
FROM tb_vehicle_network_log
WHERE iccid1 IS NOT NULL AND iccid1 != ''
AND row_valid = 1;

-- 4. 迁移日志表数据 - iccid2 (slot 2)
INSERT INTO tb_vehicle_sim_log (vin, slot_no, iccid, package_code, description, create_by, create_time, modify_by, modify_time, row_version, row_valid)
SELECT
    vin,
    2 AS slot_no,
    iccid2 AS iccid,
    package_code,
    description,
    create_by,
    create_time,
    modify_by,
    modify_time,
    row_version,
    row_valid
FROM tb_vehicle_network_log
WHERE iccid2 IS NOT NULL AND iccid2 != ''
AND row_valid = 1;
