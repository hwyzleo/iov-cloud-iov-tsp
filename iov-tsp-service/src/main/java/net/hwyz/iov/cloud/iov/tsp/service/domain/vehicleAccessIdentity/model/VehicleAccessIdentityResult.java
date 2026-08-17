package net.hwyz.iov.cloud.iov.tsp.service.domain.vehicleAccessIdentity.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ResolveReason;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ResolveStatus;

import java.time.Instant;

/**
 * 车辆接入身份解析结果（领域模型）
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleAccessIdentityResult {

    /**
     * 车辆VIN
     */
    private String vin;

    /**
     * 解析状态
     */
    private ResolveStatus status;

    /**
     * 解析原因
     */
    private ResolveReason reason;

    /**
     * HSM UID（证书CN / MQTT device identity 的规范值）
     */
    private String hsmUid;

    /**
     * TBOX 序列号（仅资产追溯，不得作为 MQTT device key）
     */
    private String tboxSn;

    /**
     * 绑定版本
     */
    private Long bindingVersion;

    /**
     * 绑定最近更新时间
     */
    private Instant bindingUpdatedAt;
}
