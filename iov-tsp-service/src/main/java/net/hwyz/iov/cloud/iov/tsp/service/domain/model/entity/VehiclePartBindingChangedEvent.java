package net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehiclePartBindingChangedEvent {

    private String vin;
    private Long bindingId;
    private String partCode;
    private String sn;
    private String deviceCategory;
    private String vehicleNodeCode;
    private String iccid1;
    private String iccid2;
    private String changeType; // BIND, UNBIND, REPLACE
    private Long replaceOfBindingId;
    private Instant occurredAt;
    private Long seq;
}
