package net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tbox {

    private Long id;

    private String sn;

    private String no;

    private String configWord;

    private String supplierCode;

    private String hardwareVer;

    private String softwareVer;

    private String hardwareNo;

    private String softwareNo;

    private String hsm;

    private String iccid1;

    private String iccid2;

    private String imei;

    private String description;

    private String createBy;

    private String modifyBy;

}