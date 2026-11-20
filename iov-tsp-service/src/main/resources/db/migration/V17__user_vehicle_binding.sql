-- V17__user_vehicle_binding.sql
-- IOV-TSP-DSN-CR-017：C 端用户绑定激活车辆——TSP 独立用户车辆关系业务模块。
-- 边界：
--   1) 本组表与 tb_vehicle_tbox/tb_vehicle_ccp（VMD 装配绑定只读投影）及 VIAM 安全投影完全独立；
--      用户—VIN 主使用人是「服务使用关系」，不是法律产权，也不驱动设备安全开通。
--   2) 时间列默认 DATETIME(3)，业务绝对时刻显式按 UTC 落库（D017-08）。
--   3) 全程不落明文二维码/令牌；二维码仅存不可逆摘要（SHA-256 十六进制）。
--   4) 不改动任何已应用脚本；本版本为新增迁移。建表使用 MySQL 原生 CREATE TABLE IF NOT EXISTS。
-- 幂等：Druid WallFilter 拒绝解析「CREATE PROCEDURE + IF NOT EXISTS ADD COLUMN」存储过程守卫，
--       故本迁移全部为新建表，不涉及 ALTER，幂等由 Flyway 版本记录保证。

-- ================= 一次性激活会话 =================
CREATE TABLE IF NOT EXISTS tb_activation_session (
    id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    session_id            VARCHAR(64)  NOT NULL COMMENT '会话业务ID（对外引用，不用自增主键）',
    qr_token_hash         VARCHAR(64)  NOT NULL COMMENT '二维码凭据不可逆摘要（SHA-256 十六进制）',
    trusted_vehicle_ref   VARCHAR(128) NULL COMMENT '可信车端引用（车端会话/设备标识不可逆引用，不含 PII）',
    vin                   VARCHAR(32)  NOT NULL COMMENT '可信车端上下文 VIN',
    agreement_revisions   VARCHAR(512) NULL COMMENT '展示的两项协议修订证据（agreementType:revisionId，逗号分隔）',
    expires_at            DATETIME(3)  NOT NULL COMMENT '会话到期绝对时刻（UTC）',
    state                 VARCHAR(24)  NOT NULL COMMENT 'ISSUED/SCANNED/BOUND/CANCELLED/EXPIRED/SKIPPED',
    claimed_account_id    VARCHAR(64)  NULL COMMENT '首次有效扫码认领账号（可信账号断言）',
    claimed_at            DATETIME(3)  NULL COMMENT '认领时间（UTC）',
    binding_operation_id  VARCHAR(64)  NULL COMMENT '会话关联的绑定操作ID',
    skipped_at            DATETIME(3)  NULL COMMENT '跳过激活时间（UTC）',
    cancelled_at          DATETIME(3)  NULL COMMENT '取消时间（UTC）',
    bound_at              DATETIME(3)  NULL COMMENT '绑定成功时间（UTC）',
    result_expires_at     DATETIME(3)  NULL COMMENT '车端结果恢复窗口截止时刻（UTC）',
    ivi_login_state       VARCHAR(16)  NULL COMMENT '车机登录结果 PENDING/SUCCEEDED/FAILED（独立于绑定状态）',
    ivi_login_report_id   VARCHAR(64)  NULL COMMENT '车机登录结果回执ID（幂等）',
    ivi_login_reported_at DATETIME(3)  NULL COMMENT '车机登录结果接收时间（UTC）',
    description           VARCHAR(255) NULL COMMENT '描述',
    create_time           DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    create_by             VARCHAR(64)  NULL COMMENT '创建者',
    modify_time           DATETIME(3)  NULL COMMENT '修改时间',
    modify_by             VARCHAR(64)  NULL COMMENT '修改者',
    row_version           INT          NOT NULL DEFAULT 1 COMMENT '乐观版本（条件更新用）',
    row_valid             TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_activation_session_id (session_id),
    UNIQUE KEY uk_activation_session_qr_token_hash (qr_token_hash),
    KEY idx_activation_session_vin (vin),
    KEY idx_activation_session_state_expires (state, expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'C 端绑车一次性激活会话（IOV-TSP-DSN-CR-017）';

-- ================= 用户—车辆主使用人关系 =================
-- active_vin 生成列 + 唯一键：每 VIN 至多一位当前主使用人（服务使用关系，非产权）。
-- MySQL 支持 STORED 生成列上的唯一索引；公共软删字段 row_valid 纳入生成条件。
CREATE TABLE IF NOT EXISTS tb_user_vehicle_relation (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    relation_id         VARCHAR(64)  NOT NULL COMMENT '关系业务ID',
    account_id          VARCHAR(64)  NOT NULL COMMENT '可信账号标识',
    vin                 VARCHAR(32)  NOT NULL COMMENT '车辆 VIN',
    role                VARCHAR(24)  NOT NULL DEFAULT 'PRIMARY_USER' COMMENT '关系角色，本期仅 PRIMARY_USER',
    state               VARCHAR(16)  NOT NULL COMMENT 'ACTIVE/RELEASED',
    bound_at            DATETIME(3)  NOT NULL COMMENT '绑定成功绝对时刻（UTC）',
    source_operation_id VARCHAR(64)  NOT NULL COMMENT '来源绑定操作ID',
    rule_version        VARCHAR(32)  NULL COMMENT '资格规则版本',
    fact_ref            VARCHAR(512) NULL COMMENT 'VSO 事实引用/版本摘要（不含 PII）',
    active_vin          VARCHAR(32)  GENERATED ALWAYS AS (
        CASE WHEN state = 'ACTIVE' AND row_valid = 1 THEN vin ELSE NULL END
    ) STORED COMMENT '当前主使用人聚合键（生成列，配合唯一键约束每个 VIN 至多一位）',
    description         VARCHAR(255) NULL COMMENT '描述',
    create_time         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    create_by           VARCHAR(64)  NULL COMMENT '创建者',
    modify_time         DATETIME(3)  NULL COMMENT '修改时间',
    modify_by           VARCHAR(64)  NULL COMMENT '修改者',
    row_version         INT          NOT NULL DEFAULT 1 COMMENT '乐观版本',
    row_valid           TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_vehicle_relation_id (relation_id),
    UNIQUE KEY uk_user_vehicle_relation_active_vin (active_vin),
    KEY idx_user_vehicle_relation_account (account_id, state),
    KEY idx_user_vehicle_relation_vin (vin, state)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户—车辆主使用人关系（IOV-TSP-DSN-CR-017）';

-- ================= 绑定操作（幂等台账） =================
CREATE TABLE IF NOT EXISTS tb_user_vehicle_binding_operation (
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    operation_id     VARCHAR(64)  NOT NULL COMMENT '操作业务ID',
    caller_scope     VARCHAR(16)  NOT NULL COMMENT '调用方范围 APP/VEHICLE',
    account_id       VARCHAR(64)  NOT NULL COMMENT '可信账号标识',
    idempotency_key  VARCHAR(128) NOT NULL COMMENT '幂等键（调用方提供）',
    request_digest   VARCHAR(64)  NOT NULL COMMENT '请求内容不可逆摘要（同键不同摘要即冲突）',
    session_id       VARCHAR(64)  NOT NULL COMMENT '会话业务ID',
    vin              VARCHAR(32)  NOT NULL COMMENT '车辆 VIN',
    status           VARCHAR(16)  NOT NULL COMMENT 'PENDING/SUCCEEDED/FAILED',
    result_reason    VARCHAR(32)  NULL COMMENT '失败/冲突语义 reason（成功时为空）',
    result_relation_id VARCHAR(64) NULL COMMENT '成功时关联的关系ID',
    eligibility      VARCHAR(32)  NULL COMMENT '资格判定 ELIGIBLE/INELIGIBLE/UNDETERMINED/DEPENDENCY_UNAVAILABLE',
    rule_version     VARCHAR(32)  NULL COMMENT '资格规则版本',
    fact_ref         VARCHAR(512) NULL COMMENT 'VSO 事实引用/版本摘要',
    fact_checked_at  DATETIME(3)  NULL COMMENT '事实读取绝对时刻（UTC）',
    completed_at     DATETIME(3)  NULL COMMENT '终态时间（UTC）',
    description      VARCHAR(255) NULL COMMENT '描述',
    create_time      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    create_by        VARCHAR(64)  NULL COMMENT '创建者',
    modify_time      DATETIME(3)  NULL COMMENT '修改时间',
    modify_by        VARCHAR(64)  NULL COMMENT '修改者',
    row_version      INT          NOT NULL DEFAULT 1 COMMENT '乐观版本',
    row_valid        TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_uvb_operation_id (operation_id),
    UNIQUE KEY uk_uvb_idempotency (caller_scope, account_id, idempotency_key),
    KEY idx_uvb_session (session_id),
    KEY idx_uvb_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户绑车确认操作幂等台账（IOV-TSP-DSN-CR-017）';

-- ================= 业务审计（一次业务迁移一行） =================
CREATE TABLE IF NOT EXISTS tb_user_vehicle_audit (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    audit_id       VARCHAR(64)  NOT NULL COMMENT '审计业务ID',
    biz_type       VARCHAR(32)  NOT NULL COMMENT 'SESSION/BINDING/CONSENT/PRIVACY/ACTIVATION_FACT',
    biz_id         VARCHAR(64)  NOT NULL COMMENT '关联业务ID（sessionId/operationId/consentId/reportId）',
    vin            VARCHAR(32)  NULL COMMENT '车辆 VIN',
    account_id     VARCHAR(64)  NULL COMMENT '可信账号标识（无账号场景为空）',
    from_state     VARCHAR(32)  NULL COMMENT '迁移前状态',
    to_state       VARCHAR(32)  NULL COMMENT '迁移后状态',
    operator_source VARCHAR(32) NOT NULL COMMENT '可信操作者来源 APP/VEHICLE/SYSTEM',
    reason         VARCHAR(64)  NULL COMMENT '原因/失败语义 reason',
    fact_ref       VARCHAR(512) NULL COMMENT '事实引用/版本摘要',
    occurred_at    DATETIME(3)  NOT NULL COMMENT '业务迁移绝对时刻（UTC）',
    description    VARCHAR(255) NULL COMMENT '描述（不得含明文二维码/令牌）',
    create_time    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    create_by      VARCHAR(64)  NULL COMMENT '创建者',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_vehicle_audit_id (audit_id),
    KEY idx_user_vehicle_audit_biz (biz_type, biz_id),
    KEY idx_user_vehicle_audit_vin (vin, occurred_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户车辆关系业务审计（IOV-TSP-DSN-CR-017）';

-- ================= VIN 级协议同意（跳过激活） =================
CREATE TABLE IF NOT EXISTS tb_vehicle_agreement_consent (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    consent_id         VARCHAR(64)  NOT NULL COMMENT '同意记录业务ID',
    vin                VARCHAR(32)  NOT NULL COMMENT '车辆 VIN（VIN 级范围）',
    agreement_type     VARCHAR(32)  NOT NULL COMMENT 'PRIVACY_POLICY/USER_AGREEMENT',
    agreement_version  VARCHAR(64)  NULL COMMENT '展示版本',
    revision_id        VARCHAR(64)  NULL COMMENT 'CMS 不可变修订标识',
    language           VARCHAR(16)  NULL COMMENT '协议语言',
    content_hash       VARCHAR(80)  NULL COMMENT '协议内容摘要（同意证据引用）',
    source             VARCHAR(32)  NOT NULL COMMENT '来源 VEHICLE',
    report_id          VARCHAR(64)  NOT NULL COMMENT '来源回执ID（幂等键）',
    payload_digest     VARCHAR(64)  NOT NULL COMMENT '回执内容摘要（同ID不同摘要即冲突）',
    reported_agreed_at DATETIME(3)  NULL COMMENT '用户动作时间（UTC，来源提供）',
    received_at        DATETIME(3)  NOT NULL COMMENT '接收时间（UTC，不冒充用户动作时间）',
    description        VARCHAR(255) NULL COMMENT '描述',
    create_time        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    create_by          VARCHAR(64)  NULL COMMENT '创建者',
    modify_time        DATETIME(3)  NULL COMMENT '修改时间',
    modify_by          VARCHAR(64)  NULL COMMENT '修改者',
    row_version        INT          NOT NULL DEFAULT 1 COMMENT '乐观版本',
    row_valid          TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_vac_consent_id (consent_id),
    UNIQUE KEY uk_vac_scope (vin, agreement_type, revision_id, report_id),
    KEY idx_vac_vin (vin)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'VIN 级协议同意（车端跳过激活，IOV-TSP-DSN-CR-017）';

-- ================= Account + VIN 级协议同意 =================
CREATE TABLE IF NOT EXISTS tb_account_vehicle_agreement_consent (
    id                 BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    consent_id         VARCHAR(64)  NOT NULL COMMENT '同意记录业务ID',
    account_id         VARCHAR(64)  NOT NULL COMMENT '账号标识（个人范围必填，不允许空值规避唯一键）',
    vin                VARCHAR(32)  NOT NULL COMMENT '车辆 VIN',
    agreement_type     VARCHAR(32)  NOT NULL COMMENT 'PRIVACY_POLICY/USER_AGREEMENT',
    agreement_version  VARCHAR(64)  NULL COMMENT '实际展示版本（不取最新版覆盖）',
    revision_id        VARCHAR(64)  NULL COMMENT '实际展示修订标识',
    language           VARCHAR(16)  NULL COMMENT '协议语言',
    content_hash       VARCHAR(80)  NULL COMMENT '协议内容摘要（同意证据引用）',
    source             VARCHAR(32)  NOT NULL COMMENT '来源 VEHICLE',
    report_id          VARCHAR(64)  NOT NULL COMMENT '登录成功回执ID（幂等键）',
    payload_digest     VARCHAR(64)  NOT NULL COMMENT '回执内容摘要（同ID不同摘要即冲突）',
    source_session_id  VARCHAR(64)  NULL COMMENT '来源会话ID',
    binding_operation_id VARCHAR(64) NULL COMMENT '来源绑定操作ID',
    reported_agreed_at DATETIME(3)  NULL COMMENT '用户动作时间（UTC）',
    received_at        DATETIME(3)  NOT NULL COMMENT '接收时间（UTC）',
    description        VARCHAR(255) NULL COMMENT '描述',
    create_time        DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    create_by          VARCHAR(64)  NULL COMMENT '创建者',
    modify_time        DATETIME(3)  NULL COMMENT '修改时间',
    modify_by          VARCHAR(64)  NULL COMMENT '修改者',
    row_version        INT          NOT NULL DEFAULT 1 COMMENT '乐观版本',
    row_valid          TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_avac_consent_id (consent_id),
    UNIQUE KEY uk_avac_scope (account_id, vin, agreement_type, revision_id, report_id),
    KEY idx_avac_account_vin (account_id, vin)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Account+VIN 级协议同意（IOV-TSP-DSN-CR-017）';

-- ================= Account + VIN 级隐私授权 =================
CREATE TABLE IF NOT EXISTS tb_account_vehicle_privacy_authorization (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    authorization_id     VARCHAR(64)  NOT NULL COMMENT '授权记录业务ID',
    account_id           VARCHAR(64)  NOT NULL COMMENT '账号标识',
    vin                  VARCHAR(32)  NOT NULL COMMENT '车辆 VIN',
    authorization_item   VARCHAR(64)  NOT NULL COMMENT '授权项（批准目录，不在本 CR 凭空定义）',
    state                VARCHAR(16)  NOT NULL COMMENT 'GRANTED/DENIED/NOT_GRANTED',
    version              BIGINT       NOT NULL DEFAULT 0 COMMENT '来源版本（按来源顺序收敛，不用接收时间）',
    source               VARCHAR(32)  NOT NULL COMMENT '来源 VEHICLE',
    report_id            VARCHAR(64)  NOT NULL COMMENT '来源回执ID（幂等键）',
    payload_digest       VARCHAR(64)  NOT NULL COMMENT '回执内容摘要',
    reported_at          DATETIME(3)  NULL COMMENT '用户动作时间（UTC）',
    received_at          DATETIME(3)  NOT NULL COMMENT '接收时间（UTC）',
    description          VARCHAR(255) NULL COMMENT '描述',
    create_time          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    create_by            VARCHAR(64)  NULL COMMENT '创建者',
    modify_time          DATETIME(3)  NULL COMMENT '修改时间',
    modify_by            VARCHAR(64)  NULL COMMENT '修改者',
    row_version          INT          NOT NULL DEFAULT 1 COMMENT '乐观版本',
    row_valid            TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_avpa_authorization_id (authorization_id),
    UNIQUE KEY uk_avpa_item (account_id, vin, authorization_item),
    KEY idx_avpa_account_vin (account_id, vin)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'Account+VIN 隐私授权当前状态（IOV-TSP-DSN-CR-017）';

-- ================= 首次激活候选事实 + Outbox =================
-- D017-07：绑定提交、车机登录成功、VMD 首次激活是三个独立事实；
-- 本表只登记「用户绑车激活」候选事实，由 VMD 按 eventId 幂等维护整车首次生命周期节点。
CREATE TABLE IF NOT EXISTS tb_user_vehicle_activation_fact (
    id                  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    event_id            VARCHAR(64)  NOT NULL COMMENT '候选事实事件ID（VMD 幂等键）',
    schema_version      INT          NOT NULL DEFAULT 1 COMMENT '事件契约版本',
    vin                 VARCHAR(32)  NOT NULL COMMENT '车辆 VIN（Kafka Key）',
    binding_operation_id VARCHAR(64) NOT NULL COMMENT '绑定操作ID（业务键）',
    source_fact_ref     VARCHAR(512) NULL COMMENT '来源事实引用（VSO 事实/登录回执摘要，不含 PII）',
    occurred_at         DATETIME(3)  NOT NULL COMMENT '绑车激活事实发生绝对时刻（UTC）',
    payload_digest      VARCHAR(64)  NOT NULL COMMENT '事件载荷摘要（防重复事实/冲突识别）',
    publish_status      VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PUBLISHED/FAILED',
    retry_count         INT          NOT NULL DEFAULT 0 COMMENT '投递重试次数',
    last_error          VARCHAR(512) NULL COMMENT '最近投递错误摘要（脱敏）',
    published_at        DATETIME(3)  NULL COMMENT '投递成功时间（UTC）',
    description         VARCHAR(255) NULL COMMENT '描述',
    create_time         DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    create_by           VARCHAR(64)  NULL COMMENT '创建者',
    modify_time         DATETIME(3)  NULL COMMENT '修改时间',
    modify_by           VARCHAR(64)  NULL COMMENT '修改者',
    row_version         INT          NOT NULL DEFAULT 1 COMMENT '乐观版本',
    row_valid           TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_uvaf_event_id (event_id),
    UNIQUE KEY uk_uvaf_business (vin, binding_operation_id),
    KEY idx_uvaf_publish_status (publish_status, create_time)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '用户绑车首次激活候选事实与投递 Outbox（IOV-TSP-DSN-CR-017）';
