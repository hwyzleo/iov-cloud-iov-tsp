package net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleNetwork {

    private Long id;

    private String vin;

    private String iccid1;

    private Boolean iccid1Online;

    private String iccid2;

    private Boolean iccid2Online;

    private String packageCode;

    private Boolean binding;

    private Boolean auth;

    private String description;

    private String createBy;

    private String modifyBy;

    public void initDefaultState() {
        this.iccid1Online = false;
        this.iccid2Online = false;
        this.binding = true;
        this.auth = false;
    }

}