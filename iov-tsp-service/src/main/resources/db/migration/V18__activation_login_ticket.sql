-- V18__activation_login_ticket.sql
-- IOV-TSP-DSN-CR-019：车端「绑定即登录」的一次性登录票据。
-- 边界：
--   1) 仅新建本表与索引，不改动 tb_activation_session / tb_user_vehicle_relation /
--      tb_user_vehicle_activation_fact，不做数据回填。
--   2) 只保存票据原值的不可逆摘要（SHA-256 十六进制），原值不落库、不进日志；
--      时间列 DATETIME(3) 由仓储显式按 UTC 绝对时刻映射。
--   3) 同一会话同一时刻至多一张 ISSUED 票据（由重签时先置 REVOKED + issue_seq 递增保证）。
-- 幂等：建表使用 MySQL 原生 CREATE TABLE IF NOT EXISTS；本迁移不涉及 ALTER，
--       版本记录由 Flyway 保证。

CREATE TABLE IF NOT EXISTS tb_activation_login_ticket (
    id                   BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id            VARCHAR(64)  NOT NULL COMMENT '票据业务标识（对外引用，非凭据本体）',
    ticket_hash          CHAR(64)     NOT NULL COMMENT '票据原值的不可逆摘要（SHA-256 十六进制）',
    session_id           VARCHAR(64)  NOT NULL COMMENT '来源激活会话ID',
    binding_operation_id VARCHAR(64)  NOT NULL COMMENT '绑定操作ID（四要素之一）',
    vin                  VARCHAR(32)  NOT NULL COMMENT '可信车辆 VIN（四要素之一）',
    account_id           VARCHAR(64)  NOT NULL COMMENT '认领账号（四要素之一）',
    relation_id          VARCHAR(64)  NULL COMMENT '绑定关系标识（消费后返回，供令牌声明）',
    state                VARCHAR(16)  NOT NULL COMMENT 'ISSUED/CONSUMED/REVOKED/EXPIRED',
    issued_at            DATETIME(3)  NOT NULL COMMENT '签发绝对时刻（UTC）',
    expires_at           DATETIME(3)  NOT NULL COMMENT '到期绝对时刻（UTC）',
    consumed_at          DATETIME(3)  NULL COMMENT '消费绝对时刻（UTC）',
    consumed_by          VARCHAR(64)  NULL COMMENT '消费方标识（如 sec-ciam）',
    issue_seq            INT          NOT NULL DEFAULT 1 COMMENT '同一会话签发序号（重签计数）',
    description          VARCHAR(255) NULL COMMENT '描述',
    create_time          DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    create_by            VARCHAR(64)  NULL COMMENT '创建者',
    modify_time          DATETIME(3)  NULL COMMENT '修改时间',
    modify_by            VARCHAR(64)  NULL COMMENT '修改者',
    row_version          INT          NOT NULL DEFAULT 1 COMMENT '乐观版本（条件更新用）',
    row_valid            TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_id (ticket_id),
    UNIQUE KEY uk_ticket_hash (ticket_hash),
    KEY idx_ticket_session (session_id, state, expires_at),
    KEY idx_ticket_expires (state, expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '车端一次性登录票据（IOV-TSP-DSN-CR-019）';
