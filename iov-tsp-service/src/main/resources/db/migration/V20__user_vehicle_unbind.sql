-- V20__user_vehicle_unbind.sql
-- IOV-TSP-DSN-CR-021：车辆管理用户关系后台运营与人工解绑
-- 边界：
--   1) 关系释放是 TSP 本地强事务；CIAM 凭证撤销由台账补偿，不使用分布式事务。
--   2) 不改 V17～V19 已应用脚本；本版本只做增量 ALTER/CREATE。
--   3) 不物理删除关系、授权、审计、会话或首次激活历史。
--   4) 时间列 DATETIME(3)，业务绝对时刻显式按 UTC 落库（D017-08 / CR-014）。
-- 幂等：Flyway 版本记录保证；建表使用 MySQL 原生 CREATE TABLE IF NOT EXISTS。

-- ================= 关系释放列（D021-05） =================
ALTER TABLE tb_user_vehicle_relation
    ADD COLUMN released_at DATETIME(3) NULL COMMENT '关系释放绝对时刻（UTC，IOV-TSP-DSN-CR-021）'
        AFTER bound_at,
    ADD COLUMN release_reason VARCHAR(128) NULL COMMENT '解绑原因代码/摘要'
        AFTER released_at,
    ADD COLUMN release_operation_id VARCHAR(64) NULL COMMENT '解绑操作ID'
        AFTER release_reason,
    ADD COLUMN released_by VARCHAR(64) NULL COMMENT '可信 MPT 操作人'
        AFTER release_operation_id;

-- 既有 ACTIVE 行四列保持 NULL；active_vin 生成表达式与唯一键不变。

-- ================= 解绑操作与凭证撤销台账（D021-05） =================
CREATE TABLE IF NOT EXISTS tb_user_vehicle_unbind_operation (
    id                             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    operation_id                   VARCHAR(64)  NOT NULL COMMENT '解绑操作业务ID（对外引用与CIAM幂等键）',
    relation_id                    VARCHAR(64)  NOT NULL COMMENT '关系业务ID',
    account_id                     VARCHAR(64)  NOT NULL COMMENT '关系账号标识',
    vin                            VARCHAR(32)  NOT NULL COMMENT '车辆 VIN',
    operator_id                    VARCHAR(64)  NOT NULL COMMENT '可信 MPT 操作人',
    idempotency_key                VARCHAR(128) NOT NULL COMMENT '操作人提供的幂等键',
    request_digest                 CHAR(64)     NOT NULL COMMENT '请求内容不可逆摘要（同键异体即冲突 401110）',
    expected_row_version           INT          NOT NULL COMMENT '请求声明的预期关系版本',
    reason                         VARCHAR(128) NOT NULL COMMENT '解绑原因代码/摘要',
    relation_state                 VARCHAR(16)  NOT NULL COMMENT 'PENDING/RELEASED',
    released_at                    DATETIME(3)  NULL COMMENT '关系释放绝对时刻（UTC）',
    completed_at                   DATETIME(3)  NULL COMMENT '本地关系事务完成时刻（UTC）',
    credential_revocation_status   VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCEEDED/FAILED',
    revoked_session_count          INT          NULL COMMENT 'CIAM 本次实际撤销会话数',
    retry_count                    INT          NOT NULL DEFAULT 0 COMMENT '撤销重试次数',
    next_retry_at                  DATETIME(3)  NULL COMMENT '下次自动重试绝对时刻（UTC）',
    last_error                     VARCHAR(512) NULL COMMENT '最近一次撤销失败摘要（脱敏、截断）',
    description                    VARCHAR(255) NULL COMMENT '描述',
    create_time                    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    create_by                      VARCHAR(64)  NULL COMMENT '创建者',
    modify_time                    DATETIME(3)  NULL COMMENT '修改时间',
    modify_by                      VARCHAR(64)  NULL COMMENT '修改者',
    row_version                    INT          NOT NULL DEFAULT 1 COMMENT '乐观版本',
    row_valid                      TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_uvuo_operation_id (operation_id),
    UNIQUE KEY uk_uvuo_operator_key (operator_id, idempotency_key),
    KEY idx_uvuo_revocation_retry (credential_revocation_status, next_retry_at),
    KEY idx_uvuo_relation (relation_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户车辆关系人工解绑与CIAM凭证撤销台账（IOV-TSP-DSN-CR-021）';

-- ================= 隐私授权关系周期（D021-05/D021-08） =================
ALTER TABLE tb_account_vehicle_privacy_authorization
    ADD COLUMN relation_id VARCHAR(64) NULL COMMENT '授权来源关系周期ID（IOV-TSP-DSN-CR-021）'
        AFTER vin;

-- 存量可明确归属当前唯一 ACTIVE Account+VIN 关系的记录：映射该 relationId。
UPDATE tb_account_vehicle_privacy_authorization a
    JOIN tb_user_vehicle_relation r
      ON r.account_id = a.account_id
     AND r.vin = a.vin
     AND r.state = 'ACTIVE'
     AND r.row_valid = 1
SET a.relation_id = r.relation_id
WHERE a.relation_id IS NULL;

-- 无 ACTIVE 关系或无法证明归属的存量记录：进入 INVALIDATED 历史周期，不归属未来新关系。
UPDATE tb_account_vehicle_privacy_authorization
SET state = 'INVALIDATED',
    modify_time = CURRENT_TIMESTAMP(3)
WHERE relation_id IS NULL
  AND row_valid = 1
  AND state <> 'INVALIDATED';

-- 唯一粒度调整为 Account+VIN+relationId+授权项；不删除历史行。
ALTER TABLE tb_account_vehicle_privacy_authorization
    DROP INDEX uk_avpa_item,
    ADD UNIQUE KEY uk_avpa_relation_item (account_id, vin, relation_id, authorization_item);
