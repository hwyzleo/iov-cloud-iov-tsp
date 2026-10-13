-- V13__device_admission_audit.sql
-- TSP-DSN-CR-011 §9/§10：设备接入准入审计补齐（敏感标识只存不可逆摘要）

ALTER TABLE tb_device_admission_log
    ADD COLUMN uid_digest VARCHAR(64) NULL COMMENT 'uid 不可逆摘要(SHA-256)' AFTER device_status_check_result,
    ADD COLUMN cert_serial_digest VARCHAR(64) NULL COMMENT '证书序列号不可逆摘要(SHA-256)' AFTER uid_digest,
    ADD COLUMN source_ip_digest VARCHAR(64) NULL COMMENT '来源IP不可逆摘要(SHA-256)' AFTER cert_serial_digest,
    ADD COLUMN checked_at DATETIME NULL COMMENT '证书检查完成时刻' AFTER source_ip_digest,
    ADD COLUMN revocation_source VARCHAR(16) NULL COMMENT '吊销状态来源: OCSP/CRL' AFTER checked_at,
    ADD COLUMN dependency_outcome VARCHAR(64) NULL COMMENT '依赖处置结果: OK/依赖分类/未评估原因' AFTER revocation_source,
    ADD COLUMN caller VARCHAR(64) NULL COMMENT '调用方标识' AFTER dependency_outcome;

-- 旧列 hsm 停止写入明文，仅保留历史数据（deprecated）
ALTER TABLE tb_device_admission_log
    MODIFY COLUMN hsm VARCHAR(64) NULL COMMENT '设备HSM标识(deprecated: 新记录只写 uid_digest)';

CREATE INDEX idx_device_admission_uid_digest ON tb_device_admission_log (uid_digest);
CREATE INDEX idx_device_admission_checked_at ON tb_device_admission_log (checked_at);
