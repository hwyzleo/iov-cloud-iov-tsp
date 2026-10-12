package net.hwyz.iov.cloud.iov.tsp.api.vo.mpt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * TBOX 页面联动稳定标识
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TboxLinkKeysVo {

    /**
     * 设备序列号
     */
    private String sn;

    /**
     * 零件编码
     */
    private String partCode;

    /**
     * 当前 VIN（绑定冲突时为 null）
     */
    private String vin;

    /**
     * 当前 active 绑定 ID（绑定冲突时为 null）
     */
    private Long bindingId;
}
