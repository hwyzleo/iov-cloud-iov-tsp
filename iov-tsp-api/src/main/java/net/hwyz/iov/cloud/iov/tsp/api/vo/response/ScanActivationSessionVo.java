package net.hwyz.iov.cloud.iov.tsp.api.vo.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ActivationSessionState;

import java.time.Instant;

/**
 * 扫码后最小化待确认车辆信息。
 * <p>
 * 不返回车辆完整档案或他人信息；同一账号可重复获取，其他账号拒绝占用。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanActivationSessionVo {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 待确认车辆 VIN
     */
    private String vin;

    /**
     * 会话状态
     */
    private ActivationSessionState state;

    /**
     * 到期绝对时刻
     */
    private Instant expiresAt;

    /**
     * 是否已被当前账号认领
     */
    private Boolean claimed;
}
