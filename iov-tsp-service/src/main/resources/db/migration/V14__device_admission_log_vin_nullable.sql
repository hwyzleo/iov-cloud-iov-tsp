-- V14__device_admission_log_vin_nullable.sql
-- TSP-DSN-CR-011 §9：审计 vin 允许为空。
-- 在绑定解析前即 fail-closed 的分支（UID_UNKNOWN/UNBOUND/BINDING_CONFLICT，以及 PKI 依赖不可用）
-- 拿不到唯一 VIN，若强制 NOT NULL 会导致审计 INSERT 失败（Column 'vin' cannot be null）并丢失安全审计记录。
ALTER TABLE tb_device_admission_log
    MODIFY COLUMN vin VARCHAR(17) NULL COMMENT '车辆VIN（绑定解析前拒绝时为 NULL）';
