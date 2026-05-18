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
public class VehicleTbox {

    private Long id;

    private String vin;

    private String sn;

    private String description;

    private String createBy;

    private String modifyBy;

    private Instant createTime;

}