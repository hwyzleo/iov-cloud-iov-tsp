package net.hwyz.iov.cloud.iov.tsp.service.application.assembler;

import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.VehicleNetworkCmd;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleNetwork;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper
public interface VehicleNetworkAssembler {

    VehicleNetworkAssembler INSTANCE = Mappers.getMapper(VehicleNetworkAssembler.class);

    @Mapping(source = "simList", target = "iccid1", qualifiedByName = "simListToIccid1")
    @Mapping(source = "simList", target = "iccid2", qualifiedByName = "simListToIccid2")
    VehicleNetwork toEntity(VehicleNetworkCmd cmd);

    @Named("simListToIccid1")
    static String simListToIccid1(java.util.List<VehicleNetworkCmd.VehicleSimCmd> simList) {
        if (simList == null) {
            return null;
        }
        return simList.stream()
                .filter(sim -> Integer.valueOf(1).equals(sim.getSlotNo()))
                .map(VehicleNetworkCmd.VehicleSimCmd::getIccid)
                .findFirst()
                .orElse(null);
    }

    @Named("simListToIccid2")
    static String simListToIccid2(java.util.List<VehicleNetworkCmd.VehicleSimCmd> simList) {
        if (simList == null) {
            return null;
        }
        return simList.stream()
                .filter(sim -> Integer.valueOf(2).equals(sim.getSlotNo()))
                .map(VehicleNetworkCmd.VehicleSimCmd::getIccid)
                .findFirst()
                .orElse(null);
    }

}