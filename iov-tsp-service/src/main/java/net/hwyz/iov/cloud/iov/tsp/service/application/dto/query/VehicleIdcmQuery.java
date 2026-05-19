package net.hwyz.iov.cloud.iov.tsp.service.application.dto.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleIdcmQuery {

    private String vin;

    private String sn;

}