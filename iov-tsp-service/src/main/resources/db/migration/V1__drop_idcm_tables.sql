-- V1__drop_idcm_tables.sql
-- TSP-DSN-CR-002: 删除 IDCM 设备表/绑定表/接口/异常，TSP 退出 IDCM 设计层
-- 落地 TSP-REQ-CR-002

-- 删除车辆-IDCM绑定日志表
DROP TABLE IF EXISTS tb_vehicle_idcm_log;

-- 删除车辆-IDCM绑定表
DROP TABLE IF EXISTS tb_vehicle_idcm;

-- 删除IDCM设备日志表
DROP TABLE IF EXISTS tb_idcm_log;

-- 删除IDCM设备表
DROP TABLE IF EXISTS tb_idcm;

-- ============================================================
-- 基表补齐（TSP 部署热修 2026-09-01，落地于 V1 以保证在 V2 之前执行）
-- 背景：全新空库 db_tsp 上，V2 因 tb_tbox 等基表缺失而失败
--       （"Schema db_tsp contains a failed migration to version 2"）。
--       这些基表的 DDL 此前不在迁移链内、仅存在于存量环境；
--       追加到 V1 使全新库能自洽完成建表。
-- 说明：CREATE TABLE IF NOT EXISTS —— 存量库 V1 已应用不重跑，幂等安全；
--       结构与现有环境 SHOW CREATE TABLE 对齐（2026-09-01 本地 db_tsp 校准）。
-- ============================================================

-- TBOX 设备信息表（原始形态；device_status/activate_time/idx_hsm 由 V2/V3 追加）
CREATE TABLE IF NOT EXISTS tb_tbox (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    sn VARCHAR(255) NOT NULL COMMENT '序列号',
    no VARCHAR(255) DEFAULT NULL COMMENT '零件编号',
    config_word VARCHAR(255) DEFAULT NULL COMMENT '配置字',
    supplier_code VARCHAR(255) DEFAULT NULL COMMENT '供应商编码',
    hardware_ver VARCHAR(255) DEFAULT NULL COMMENT '硬件版本号',
    software_ver VARCHAR(255) DEFAULT NULL COMMENT '软件版本号',
    hardware_no VARCHAR(255) DEFAULT NULL COMMENT '硬件零件号',
    software_no VARCHAR(255) DEFAULT NULL COMMENT '软件零件号',
    hsm VARCHAR(100) DEFAULT NULL COMMENT '硬件安全模块(HSM标识)',
    iccid1 VARCHAR(50) NOT NULL COMMENT '集成电路卡识别码1',
    iccid2 VARCHAR(50) DEFAULT NULL COMMENT '集成电路卡识别码2',
    imei VARCHAR(20) DEFAULT NULL COMMENT '国际移动设备识别码',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_by VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    row_version INT DEFAULT 1 COMMENT '乐观锁版本',
    row_valid TINYINT DEFAULT 1 COMMENT '逻辑删除标记: 0-已删除, 1-有效',
    PRIMARY KEY (id),
    UNIQUE KEY sn (sn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车联终端(TBOX)设备信息表';

-- CCP 设备信息表
CREATE TABLE IF NOT EXISTS tb_ccp (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    sn VARCHAR(255) NOT NULL COMMENT '序列号',
    no VARCHAR(255) DEFAULT NULL COMMENT '零件编号',
    config_word VARCHAR(255) DEFAULT NULL COMMENT '配置字',
    supplier_code VARCHAR(255) DEFAULT NULL COMMENT '供应商编码',
    hardware_ver VARCHAR(255) DEFAULT NULL COMMENT '硬件版本号',
    software_ver VARCHAR(255) DEFAULT NULL COMMENT '软件版本号',
    hardware_no VARCHAR(255) DEFAULT NULL COMMENT '硬件零件号',
    software_no VARCHAR(255) DEFAULT NULL COMMENT '软件零件号',
    hsm VARCHAR(100) DEFAULT NULL COMMENT '硬件安全模块(HSM标识)',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_by VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    row_version INT DEFAULT 1 COMMENT '乐观锁版本',
    row_valid TINYINT DEFAULT 1 COMMENT '逻辑删除标记: 0-已删除, 1-有效',
    PRIMARY KEY (id),
    UNIQUE KEY sn (sn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='中央计算平台(CCP)设备信息表';

-- 车辆-TBOX 绑定投影表（原始形态；V4 追加投影治理列 + uk_binding_id）
CREATE TABLE IF NOT EXISTS tb_vehicle_tbox (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    vin VARCHAR(20) NOT NULL COMMENT '车架号',
    sn VARCHAR(255) NOT NULL COMMENT '序列号',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_by VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    row_version INT DEFAULT 1 COMMENT '乐观锁版本',
    row_valid TINYINT DEFAULT 1 COMMENT '逻辑删除标记: 0-已删除, 1-有效',
    PRIMARY KEY (id),
    UNIQUE KEY vin (vin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆-TBOX绑定投影表';

-- 车辆-CCP 绑定投影表（原始形态；V4 追加投影治理列 + uk_binding_id）
CREATE TABLE IF NOT EXISTS tb_vehicle_ccp (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    vin VARCHAR(20) NOT NULL COMMENT '车架号',
    sn VARCHAR(255) NOT NULL COMMENT '序列号',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_by VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    row_version INT DEFAULT 1 COMMENT '乐观锁版本',
    row_valid TINYINT DEFAULT 1 COMMENT '逻辑删除标记: 0-已删除, 1-有效',
    PRIMARY KEY (id),
    UNIQUE KEY vin (vin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆-CCP绑定投影表';

-- 车辆网联信息表（旧表，供 V6 迁移读取；全新库为 0 行，V7 会 DROP）
CREATE TABLE IF NOT EXISTS tb_vehicle_network (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    vin VARCHAR(20) NOT NULL COMMENT '车架号',
    iccid1 VARCHAR(50) DEFAULT NULL COMMENT '集成电路卡识别码1',
    iccid1_online TINYINT DEFAULT 0 COMMENT '卡1在线状态',
    iccid2 VARCHAR(50) DEFAULT NULL COMMENT '集成电路卡识别码2',
    iccid2_online TINYINT DEFAULT 0 COMMENT '卡2在线状态',
    package_code VARCHAR(50) DEFAULT NULL COMMENT '网联套餐编码',
    binding TINYINT DEFAULT 0 COMMENT '绑定状态: 0-已解绑, 1-已绑定',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_by VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    row_version INT DEFAULT 1 COMMENT '乐观锁版本',
    row_valid TINYINT DEFAULT 1 COMMENT '逻辑删除标记: 0-已删除, 1-有效',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆网联信息表(旧, 由V7删除)';

-- 车辆网联日志表（旧表，V6 迁移日志数据读取；V7 会 DROP）
CREATE TABLE IF NOT EXISTS tb_vehicle_network_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    vin VARCHAR(20) NOT NULL COMMENT '车架号',
    iccid1 VARCHAR(50) DEFAULT NULL COMMENT '集成电路卡识别码1',
    iccid1_online TINYINT DEFAULT 0 COMMENT '卡1在线状态',
    iccid2 VARCHAR(50) DEFAULT NULL COMMENT '集成电路卡识别码2',
    iccid2_online TINYINT DEFAULT 0 COMMENT '卡2在线状态',
    package_code VARCHAR(50) DEFAULT NULL COMMENT '网联套餐编码',
    binding TINYINT DEFAULT 0 COMMENT '绑定状态: 0-已解绑, 1-已绑定',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_by VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    row_version INT DEFAULT 1 COMMENT '乐观锁版本',
    row_valid TINYINT DEFAULT 1 COMMENT '逻辑删除标记: 0-已删除, 1-有效',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆网联日志表(旧, 由V7删除)';

-- TBOX 日志表（基线表，无迁移创建；结构与本地存量库对齐）
CREATE TABLE IF NOT EXISTS tb_tbox_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    sn VARCHAR(255) NOT NULL COMMENT '序列号',
    config_word VARCHAR(255) DEFAULT NULL COMMENT '配置字',
    hardware_ver VARCHAR(255) DEFAULT NULL COMMENT '硬件版本号',
    software_ver VARCHAR(255) DEFAULT NULL COMMENT '软件版本号',
    hardware_no VARCHAR(255) DEFAULT NULL COMMENT '硬件零件号',
    software_no VARCHAR(255) DEFAULT NULL COMMENT '软件零件号',
    hsm VARCHAR(100) DEFAULT NULL COMMENT '硬件安全模块(HSM标识)',
    iccid1 VARCHAR(50) DEFAULT NULL COMMENT '集成电路卡识别码1',
    iccid2 VARCHAR(50) DEFAULT NULL COMMENT '集成电路卡识别码2',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_by VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    row_version INT DEFAULT 1 COMMENT '乐观锁版本',
    row_valid TINYINT DEFAULT 1 COMMENT '逻辑删除标记: 0-已删除, 1-有效',
    PRIMARY KEY (id),
    KEY idx_sn (sn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='TBOX日志表';

-- CCP 日志表（基线表）
CREATE TABLE IF NOT EXISTS tb_ccp_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    sn VARCHAR(255) NOT NULL COMMENT '序列号',
    config_word VARCHAR(255) DEFAULT NULL COMMENT '配置字',
    hardware_ver VARCHAR(255) DEFAULT NULL COMMENT '硬件版本号',
    software_ver VARCHAR(255) DEFAULT NULL COMMENT '软件版本号',
    hardware_no VARCHAR(255) DEFAULT NULL COMMENT '硬件零件号',
    software_no VARCHAR(255) DEFAULT NULL COMMENT '软件零件号',
    hsm VARCHAR(100) DEFAULT NULL COMMENT '硬件安全模块(HSM标识)',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_by VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    row_version INT DEFAULT 1 COMMENT '乐观锁版本',
    row_valid TINYINT DEFAULT 1 COMMENT '逻辑删除标记: 0-已删除, 1-有效',
    PRIMARY KEY (id),
    KEY idx_sn (sn)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='CCP日志表';

-- 车辆-TBOX 绑定日志表（基线表）
CREATE TABLE IF NOT EXISTS tb_vehicle_tbox_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    vin VARCHAR(20) NOT NULL COMMENT '车架号',
    sn VARCHAR(255) NOT NULL COMMENT '序列号',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_by VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    row_version INT DEFAULT 1 COMMENT '乐观锁版本',
    row_valid TINYINT DEFAULT 1 COMMENT '逻辑删除标记: 0-已删除, 1-有效',
    PRIMARY KEY (id),
    KEY idx_vin (vin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆-TBOX绑定日志表';

-- 车辆-CCP 绑定日志表（基线表）
CREATE TABLE IF NOT EXISTS tb_vehicle_ccp_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    vin VARCHAR(20) NOT NULL COMMENT '车架号',
    sn VARCHAR(255) NOT NULL COMMENT '序列号',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_by VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    row_version INT DEFAULT 1 COMMENT '乐观锁版本',
    row_valid TINYINT DEFAULT 1 COMMENT '逻辑删除标记: 0-已删除, 1-有效',
    PRIMARY KEY (id),
    KEY idx_vin (vin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='车辆-CCP绑定日志表';

-- 指令记录表（基线表）
CREATE TABLE IF NOT EXISTS tb_cmd_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    vin VARCHAR(20) NOT NULL COMMENT '车架号',
    cmd_id VARCHAR(64) NOT NULL COMMENT '指令ID',
    cmd_type VARCHAR(50) NOT NULL COMMENT '指令类型',
    cmd_param JSON DEFAULT NULL COMMENT '指令参数',
    msg_flow TINYINT DEFAULT NULL COMMENT '消息流向: 1-下行, 2-上行',
    msg_retry SMALLINT DEFAULT NULL COMMENT '消息重试次数',
    msg_time TIMESTAMP NULL DEFAULT NULL COMMENT '消息时间',
    msg_ack_time TIMESTAMP NULL DEFAULT NULL COMMENT 'ACK时间',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    modify_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_by VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    row_version INT DEFAULT 1 COMMENT '乐观锁版本',
    row_valid TINYINT DEFAULT 1 COMMENT '逻辑删除标记: 0-已删除, 1-有效',
    PRIMARY KEY (id),
    UNIQUE KEY cmd_id (cmd_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='指令记录表';
