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
public class SimLog {

    private Long id;

    private String iccid;

    private SimState simState;

    private Boolean smsAbility;

    private Boolean dataAbility;

    private Boolean voiceAbility;

    private String description;

}