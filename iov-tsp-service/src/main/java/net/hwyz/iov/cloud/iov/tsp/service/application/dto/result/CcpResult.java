package net.hwyz.iov.cloud.iov.tsp.service.application.dto.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CcpResult {

    private String sn;

    private String no;

    private String configWord;

    private String hardwareVer;

    private String softwareVer;

    private String hardwareNo;

    private String softwareNo;

    private String hsm;

    private String supplierCode;

}