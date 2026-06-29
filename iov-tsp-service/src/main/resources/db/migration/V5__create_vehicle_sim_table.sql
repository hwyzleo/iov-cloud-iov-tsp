-- V5__create_vehicle_sim_table.sql

-- 1. 创建 tb_vehicle_sim 表（按卡一行）
CREATE TABLE IF NOT EXISTS tb_vehicle_sim (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    vin VARCHAR(17) NOT NULL COMMENT '车架号',
    slot_no TINYINT NOT NULL COMMENT '卡槽序号(1..N)',
    iccid VARCHAR(20) NOT NULL COMMENT 'SIM卡号',
    sim_status TINYINT DEFAULT NULL COMMENT 'SIM卡生命周期状态(测试/库存/激活等, CCS投影)',
    realname_status TINYINT DEFAULT NULL COMMENT '实名态(CCS投影)',
    package_code VARCHAR(50) DEFAULT NULL COMMENT '套餐编码(本期不投影, 预留)',
    source_version BIGINT DEFAULT NULL COMMENT '已应用的CCS SIM事件version, 幂等去重/乱序丢弃',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_by VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    row_version INT DEFAULT 0 COMMENT '行版本号',
    row_valid TINYINT(1) DEFAULT 1 COMMENT '有效标记(1:有效 0:无效)',
    UNIQUE KEY uk_iccid (iccid),
    INDEX idx_vin (vin)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆SIM卡表(按卡一行)';

-- 2. 创建 tb_vehicle_sim_log 审计表
CREATE TABLE IF NOT EXISTS tb_vehicle_sim_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    vin VARCHAR(17) NOT NULL COMMENT '车架号',
    slot_no TINYINT NOT NULL COMMENT '卡槽序号',
    iccid VARCHAR(20) NOT NULL COMMENT 'SIM卡号',
    sim_status TINYINT DEFAULT NULL COMMENT 'SIM卡生命周期状态',
    realname_status TINYINT DEFAULT NULL COMMENT '实名态',
    package_code VARCHAR(50) DEFAULT NULL COMMENT '套餐编码',
    source_version BIGINT DEFAULT NULL COMMENT '已应用的CCS SIM事件version',
    description VARCHAR(255) DEFAULT NULL COMMENT '描述',
    create_by VARCHAR(64) DEFAULT NULL COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_by VARCHAR(64) DEFAULT NULL COMMENT '修改者',
    modify_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    row_version INT DEFAULT 0 COMMENT '行版本号',
    row_valid TINYINT(1) DEFAULT 1 COMMENT '有效标记',
    INDEX idx_vin (vin),
    INDEX idx_iccid (iccid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='车辆SIM卡审计日志表';
