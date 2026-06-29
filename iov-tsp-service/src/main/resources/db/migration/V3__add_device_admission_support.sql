-- V3__add_device_admission_support.sql

-- 1. 修改 tb_tbox 表，新增 device_status 字段
ALTER TABLE tb_tbox ADD COLUMN device_status TINYINT NOT NULL DEFAULT 1 COMMENT '设备状态: 1-活跃, 2-未激活, 3-暂停, 4-停用';

-- 2. 新增 hsm 索引
CREATE INDEX idx_hsm ON tb_tbox(hsm);

-- 3. 创建审计日志表
CREATE TABLE tb_device_admission_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vin VARCHAR(17) NOT NULL COMMENT '车辆VIN',
    hsm VARCHAR(64) NOT NULL COMMENT '设备HSM标识',
    admission_result VARCHAR(10) NOT NULL COMMENT '裁决结果: ALLOW/DENY',
    reason VARCHAR(255) COMMENT '裁决原因',
    hsm_check_result VARCHAR(10) COMMENT 'HSM绑定检查结果: PASS/FAIL/ERROR',
    pki_check_result VARCHAR(10) COMMENT 'PKI吊销检查结果: PASS/FAIL/ERROR',
    device_status_check_result VARCHAR(10) COMMENT '设备状态检查结果: PASS/FAIL/ERROR',
    request_context TEXT COMMENT '请求上下文JSON',
    response_time_ms INT COMMENT '响应时间(毫秒)',
    description VARCHAR(255) COMMENT '描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    create_by VARCHAR(64) COMMENT '创建人',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    modify_by VARCHAR(64) COMMENT '修改人',
    row_version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    row_valid TINYINT NOT NULL DEFAULT 1 COMMENT '逻辑删除标记: 0-已删除, 1-有效',
    INDEX idx_vin (vin),
    INDEX idx_hsm (hsm),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备接入鉴权审计日志表';