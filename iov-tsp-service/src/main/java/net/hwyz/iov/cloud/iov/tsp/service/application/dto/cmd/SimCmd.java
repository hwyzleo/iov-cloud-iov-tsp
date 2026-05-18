package net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimCmd {

    private Long id;

    private String iccid;

    private String imsi;

    private String msisdn;

    private String mnoCode;

    private Integer simState;

    private Boolean smsAbility;

    private Boolean dataAbility;

    private Boolean voiceAbility;

    private String description;

    private String createBy;

    private String modifyBy;

}