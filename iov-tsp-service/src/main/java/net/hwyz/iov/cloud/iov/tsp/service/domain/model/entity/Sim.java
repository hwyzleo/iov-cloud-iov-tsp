package net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.enums.SimState;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sim {

    private Long id;

    private String iccid;

    private String imsi;

    private String msisdn;

    private String mnoCode;

    private SimState simState;

    private Boolean smsAbility;

    private Boolean dataAbility;

    private Boolean voiceAbility;

    private String description;

    private String createBy;

    private String modifyBy;

    public void activate() {
        this.simState = SimState.ACTIVE;
    }

    public void deactivate() {
        this.simState = SimState.TEST;
    }

    public void enableAllAbilities() {
        this.smsAbility = true;
        this.dataAbility = true;
        this.voiceAbility = true;
    }

}