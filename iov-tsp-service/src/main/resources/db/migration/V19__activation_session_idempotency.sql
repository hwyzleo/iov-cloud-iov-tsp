-- V19__activation_session_idempotency.sql
-- IOV-TSP-DSN-CR-020 D020-04：车端会话创建幂等台账（Idempotency-Key）。
-- 边界与幂等策略：
--   1) 仅新建表 + 索引，不给 tb_activation_session 加列（ALTER 会破坏幂等台账与会话解耦）；
--   2) 唯一键 (caller_scope, vin, idempotency_key) 为并发最终防线（§13 裁决 C / R-3），
--      caller_scope 本期固定 VEHICLE，为后续测试/运营工具等调用方预留作用域；
--   3) expires_at = 会话 expiresAt（tsp.user-vehicle.session-ttl），与「会话 TTL 到期后 key 可释放」对齐，
--      不引入第二套 TTL 配置；时间列为 DATETIME(3)，业务绝对时刻显式按 UTC 落库（D017-08 / CR-014）；
--   4) 无 ALTER、无数据回填、不改既有表；建表使用 MySQL 原生 CREATE TABLE IF NOT EXISTS，
--      幂等由 Flyway 版本记录 + IF NOT EXISTS 共同保证（Druid WallFilter 不接受存储过程守卫）。

CREATE TABLE IF NOT EXISTS tb_activation_session_idempotency (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    idempotency_id  VARCHAR(64)  NOT NULL COMMENT '幂等记录业务ID',
    caller_scope    VARCHAR(16)  NOT NULL COMMENT '调用方作用域（本期固定 VEHICLE，对齐 V17 binding_operation）',
    vin             VARCHAR(32)  NOT NULL COMMENT '车辆 VIN（幂等作用域）',
    idempotency_key VARCHAR(128) NOT NULL COMMENT '调用方提供的幂等键',
    request_digest  CHAR(64)     NOT NULL COMMENT '请求内容不可逆摘要（同键异体即冲突 401110）',
    session_id      VARCHAR(64)  NOT NULL COMMENT '复用/新建的会话业务ID',
    expires_at      DATETIME(3)  NOT NULL COMMENT '幂等释放时刻（UTC，= 会话 expiresAt）',
    description     VARCHAR(255) NULL COMMENT '描述',
    create_time     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间',
    create_by       VARCHAR(64)  NULL COMMENT '创建者',
    modify_time     DATETIME(3)  NULL COMMENT '修改时间',
    modify_by       VARCHAR(64)  NULL COMMENT '修改者',
    row_version     INT          NOT NULL DEFAULT 1 COMMENT '乐观版本',
    row_valid       TINYINT      NOT NULL DEFAULT 1 COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_asi_idempotency_id (idempotency_id),
    UNIQUE KEY uk_asi_scope_vin_key (caller_scope, vin, idempotency_key),
    KEY idx_asi_expires (expires_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '车端会话创建幂等台账（IOV-TSP-DSN-CR-020）';
