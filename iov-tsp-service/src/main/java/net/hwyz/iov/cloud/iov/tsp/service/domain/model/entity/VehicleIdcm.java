package net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleIdcm {

    private Long id;

    private String vin;

    private String sn;

    private String description;

    private String createBy;

    private String modifyBy;

    public void bindIdcm(String sn) {
        this.sn = sn;
    }

    public boolean isAlreadyBindTo(String sn) {
        return this.sn != null && this.sn.equalsIgnoreCase(sn);
    }

    public boolean hasBindOtherIdcm(String sn) {
        return this.sn != null && !this.sn.equalsIgnoreCase(sn);
    }

}