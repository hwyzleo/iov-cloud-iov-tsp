package net.hwyz.iov.cloud.iov.tsp.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 对外服务中央计算平台信息
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CcpVo {

    /**
     * 序列号
     */
    private String sn;

    /**
     * 零件编号
     */
    private String no;

    /**
     * 配置字
     */
    private String configWord;

    /**
     * 硬件版本号
     */
    private String hardwareVer;

    /**
     * 软件版本号
     */
    private String softwareVer;

    /**
     * 硬件零件号
     */
    private String hardwareNo;

    /**
     * 软件零件号
     */
    private String softwareNo;

    /**
     * 硬件安全模块
     */
    private String hsm;

}
