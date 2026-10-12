package net.hwyz.iov.cloud.iov.tsp.api.vo.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * VAGW 设备上线事件（Topic vagw.device.online，Kafka Key=hsmUid）
 * <p>
 * TSP-DSN-CR-010：与 EDD-VAGW-DSN-CR-012 契约同构，固定使用 eventType/occurredAt，
 * 与旧 vagw-tbox-event 的 type/ts 不兼容。hsmUid 为规范接入身份；vin/tboxSn 仅用于交叉校验，
 * 不改变 TSP 本地设备与绑定事实的权威边界。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VagwDeviceOnlineEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 事件类型，固定 DEVICE_ONLINE
     */
    public static final String TYPE = "DEVICE_ONLINE";

    /**
     * occurredAt 来源：EMQX 连接建立时间
     */
    public static final String TIME_SOURCE_EMQX = "EMQX";

    /**
     * occurredAt 来源：VAGW 接收时间（EMQX 时间缺失或明显越界）
     */
    public static final String TIME_SOURCE_VAGW_RECEIVED = "VAGW_RECEIVED";

    /**
     * 事件唯一标识（幂等身份）
     */
    private String eventId;

    /**
     * 事件类型
     */
    private String eventType;

    /**
     * 规范设备身份（证书 CN / MQTT device identity，同时为 Kafka Key）
     */
    private String hsmUid;

    /**
     * 车架号（交叉校验字段，非权威）
     */
    private String vin;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * VAGW 连接代际
     */
    private long connectionGeneration;

    /**
     * 设备证书序列号（可选）
     */
    private String certSerial;

    /**
     * TBOX 序列号（可选，仅资产追溯，非权威）
     */
    private String tboxSn;

    /**
     * 事件发生时间
     */
    private Instant occurredAt;

    /**
     * occurredAt 来源，EMQX/VAGW_RECEIVED
     */
    private String timeSource;
}
