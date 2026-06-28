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
public class VehicleCcp {

    private Long id;
    private String vin;
    private String sn;
    private Long bindingId;
    private String partCode;
    private String vehicleNodeCode;
    private String deviceCategory;
    private String bindState;
    private Long bindingVersion;
    private Instant lastEventTime;
    private String source;
    private String description;
    private String createBy;
    private String modifyBy;
    private Instant createTime;
}
