package net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleNetworkCmd {

    private String vin;

    private String description;

    private List<VehicleSimCmd> simList;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleSimCmd {
        private Integer slotNo;
        private String iccid;
    }

}