-- V16__viam_security_projection.sql
-- IOV-TSP-DSN-CR-016：TSP 安全职责迁出 VIAM，tb_tbox 安全列改 VIAM 只读投影。
-- 1) tb_tbox 新增投影元数据列：
--    * viam_sync_status      投影同步/完整性状态（UNKNOWN/SYNCED/PARTIAL/STALE），
--                            与「来源=VIAM」分离表达；NULL=从未同步（投影缺失）
--    * viam_last_reconciled_at 最后成功对账/快照应用时间（≠事实时间）
--    * viam_security_version VIAM 安全版本（当前 VIAM 快照 API 未提供→预留，V-GAP-1；NULL=未知）
--    * operational_retired_at  TSP 运营退役时间（独立表达，与安全 BLOCKED/RETIRED 分离）
-- 2) tb_tsp_viam_inbox：VIAM 安全事件消费幂等 Inbox。
--    不存敏感 payload（HSM UID/VIN/sessionId/certSerial 不落库），仅存契约摘要与处理状态；
--    payload 由 Kafka 重投/重放提供，重试不依赖 Inbox 原文。
-- 边界：
--    * 资产写入（导入/通用更新）不得覆盖安全投影列（配套代码：insertPo/updatePo 移除安全列）；
--      唯一业务写入者 = VIAM 安全投影处理链。
--    * 不修改已应用脚本；本版本为新增迁移，旧安全专属历史表（tb_device_admission_log /
--      tb_tbox_registration_event）本次不 DROP，保持历史证据只读。
-- 幂等说明（SEC-VIAM V2 同款约束）：Druid WallFilter 拒绝解析「CREATE PROCEDURE + IF NOT EXISTS
--   ADD COLUMN」存储过程守卫，故本迁移使用标准 ALTER TABLE ADD COLUMN，幂等由 Flyway 版本记录保证
--   （本迁移仅执行一次；失败修复后重跑）。建表使用 MySQL 原生 CREATE TABLE IF NOT EXISTS（WallFilter 放行）。

-- ================= tb_tbox 投影元数据列 =================
ALTER TABLE tb_tbox ADD COLUMN viam_sync_status VARCHAR(16) NULL
    COMMENT 'VIAM 投影同步状态: UNKNOWN/SYNCED/PARTIAL/STALE，NULL=从未同步';
ALTER TABLE tb_tbox ADD COLUMN viam_last_reconciled_at DATETIME NULL
    COMMENT 'VIAM 投影最后成功对账/快照应用时间（≠事实时间）';
ALTER TABLE tb_tbox ADD COLUMN viam_security_version BIGINT NULL
    COMMENT 'VIAM 安全状态版本（预留；当前 VIAM 快照 API 未提供，NULL=未知）';
ALTER TABLE tb_tbox ADD COLUMN operational_retired_at DATETIME NULL
    COMMENT 'TSP 运营退役时间（独立表达，与安全 BLOCKED/RETIRED 分离；NULL=未运营退役）';

-- ================= VIAM 安全事件幂等 Inbox =================
CREATE TABLE IF NOT EXISTS tb_tsp_viam_inbox (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    event_id VARCHAR(64) NOT NULL COMMENT 'VIAM 事件唯一ID（幂等键）',
    topic VARCHAR(128) NOT NULL COMMENT '来源 Kafka Topic',
    event_type VARCHAR(64) NOT NULL COMMENT '事件类型（官方契约稳定值）',
    event_key VARCHAR(128) NULL COMMENT 'Kafka Key（principalId 或 deviceSn）',
    principal_id VARCHAR(64) NULL COMMENT 'VIAM 安全主体ID',
    hsm_uid VARCHAR(64) NULL COMMENT '归一化 HSM UID（资产关联键）',
    device_sn VARCHAR(64) NULL COMMENT '设备序列号',
    contract_digest VARCHAR(64) NULL COMMENT 'payload 不可逆摘要（不存敏感原文）',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSED/RETRYABLE/CONFLICT/FAILED',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    error VARCHAR(512) NULL COMMENT '最近处理错误摘要（脱敏，不含敏感字段）',
    received_at DATETIME NOT NULL COMMENT '接收时间',
    processed_at DATETIME NULL COMMENT '处理完成时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    modify_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    create_by VARCHAR(64) NULL COMMENT '创建者',
    modify_by VARCHAR(64) NULL COMMENT '修改者',
    UNIQUE KEY uk_tsp_viam_inbox_event_id (event_id),
    KEY idx_tsp_viam_inbox_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'TSP 消费 VIAM 安全事件幂等 Inbox（IOV-TSP-DSN-CR-016）';
