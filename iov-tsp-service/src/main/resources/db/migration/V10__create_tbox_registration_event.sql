-- V10__create_tbox_registration_event.sql
-- TSP-DSN-CR-008: TBOX 注册激活事件消费幂等记录（轻量 Inbox）
-- 保存 eventId/requestId/deviceSn/eventType/occurredAt/processedAt/result 与契约不可逆摘要；
-- 建立 UK(eventId)、UK(eventType, requestId)；不保存 keyId 列表或密钥材料。

CREATE TABLE IF NOT EXISTS tb_tbox_registration_event (
    id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    event_id        VARCHAR(64)  NOT NULL COMMENT '事件唯一标识',
    request_id      VARCHAR(128) NULL COMMENT 'TBOX 稳定请求ID（V2C 聚合完成事件）',
    device_sn       VARCHAR(64)  NULL COMMENT '设备序列号/规范身份',
    event_type      VARCHAR(64)  NOT NULL COMMENT '事件类型',
    occurred_at     DATETIME     NULL COMMENT '事件发生时间',
    processed_at    DATETIME     NULL COMMENT '处理时间',
    result          VARCHAR(32)  NULL COMMENT '处理结果',
    contract_digest VARCHAR(64)  NULL COMMENT '事件契约不可逆摘要（不含 keyId）',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_reg_event_id (event_id),
    UNIQUE KEY uk_reg_event_type_request (event_type, request_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'TBOX注册激活事件消费幂等记录';
