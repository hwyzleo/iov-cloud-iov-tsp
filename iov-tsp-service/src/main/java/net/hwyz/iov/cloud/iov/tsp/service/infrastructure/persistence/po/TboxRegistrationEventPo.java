package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * <p>
 * TBOX 注册激活事件消费幂等记录 数据对象
 * </p>
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TboxRegistrationEventPo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 事件唯一标识
     */
    private String eventId;

    /**
     * 请求ID
     */
    private String requestId;

    /**
     * 设备序列号/规范身份
     */
    private String deviceSn;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 事件发生时间
     */
    private LocalDateTime occurredAt;

    /**
     * 处理时间
     */
    private LocalDateTime processedAt;

    /**
     * 处理结果
     */
    private String result;

    /**
     * 事件契约不可逆摘要（不含 keyId）
     */
    private String contractDigest;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
