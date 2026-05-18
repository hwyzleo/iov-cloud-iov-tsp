package net.hwyz.iov.cloud.iov.tsp.service.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimQuery {

    private Long id;

    private String iccid;

    private String imsi;

    private String msisdn;

    private String mnoCode;

    private Integer simState;

    private Boolean smsAbility;

    private Boolean dataAbility;

    private Boolean voiceAbility;

    private Date beginTime;

    private Date endTime;

}