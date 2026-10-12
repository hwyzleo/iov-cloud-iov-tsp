package net.hwyz.iov.cloud.iov.tsp.api.vo.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

/**
 * TBOX 设备上线事件（VAGW -> TSP）
 * <p>
 * VAGW 在 TBOX 完成 mTLS 与 US-017 准入 ALLOW 后上报，事件携带认证会话中的规范设备身份。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TboxDeviceOnlineEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 事件ID，用于幂等去重
     */
    private String eventId;

    /**
     * 规范设备身份（证书 CN / MQTT device identity）
     */
    private String hsmUid;

    /**
     * TBOX 序列号（仅用于资产追溯）
     */
    private String tboxSn;

    /**
     * 车架号
     */
    private String vin;

    /**
     * 设备证书序列号
     */
    private String certSerial;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 事件时间
     */
    private Instant ts;

}
