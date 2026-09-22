package net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TboxImportCmd {

    private String sn;

    private String no;

    private String configWord;

    private String hardwareVer;

    private String softwareVer;

    private String hardwareNo;

    private String softwareNo;

    private String hsm;

    /**
     * 设备状态: 1-待激活, 2-在役, 3-报废, 4-冻结黑名单；为空默认待激活
     */
    private Integer deviceStatus;

    private String iccid1;

    private String iccid2;

    private String imei;

}