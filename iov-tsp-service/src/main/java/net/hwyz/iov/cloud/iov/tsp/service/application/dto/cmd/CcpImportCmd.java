package net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CcpImportCmd {

    private String sn;

    private String no;

    private String configWord;

    private String hardwareVer;

    private String softwareVer;

    private String hardwareNo;

    private String softwareNo;

    private String hsm;

}