package net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdcmLog {

    private Long id;

    private String sn;

    private String configWord;

    private String hardwareVer;

    private String softwareVer;

    private String hardwareNo;

    private String softwareNo;

    private String hsm;

    private String mac;

    private String description;

}