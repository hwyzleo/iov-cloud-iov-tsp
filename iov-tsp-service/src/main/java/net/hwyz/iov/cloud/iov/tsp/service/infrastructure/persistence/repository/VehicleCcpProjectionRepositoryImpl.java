package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleCcp;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleCcpProjectionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleCcpMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleCcpPo;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class VehicleCcpProjectionRepositoryImpl implements VehicleCcpProjectionRepository {

    private final VehicleCcpMapper vehicleCcpMapper;

    @Override
    public int upsertByBindingId(VehicleCcp vehicleCcp) {
        VehicleCcpPo po = toPo(vehicleCcp);
        return vehicleCcpMapper.upsertByBindingId(po);
    }

    @Override
    public VehicleCcp getByBindingId(Long bindingId) {
        VehicleCcpPo po = vehicleCcpMapper.selectByBindingId(bindingId);
        return po != null ? toEntity(po) : null;
    }

    @Override
    public List<VehicleCcp> getAllActiveBindings() {
        List<VehicleCcpPo> poList = vehicleCcpMapper.selectAllActiveBindings();
        return poList.stream().map(this::toEntity).toList();
    }

    @Override
    public VehicleCcp getByVin(String vin) {
        List<VehicleCcpPo> poList = vehicleCcpMapper.selectPoByExample(
            VehicleCcpPo.builder().vin(vin).bindState("ACTIVE").build()
        );
        return poList.isEmpty() ? null : toEntity(poList.get(0));
    }

    @Override
    public VehicleCcp getByVinAndSn(String vin, String sn) {
        List<VehicleCcpPo> poList = vehicleCcpMapper.selectPoByExample(
            VehicleCcpPo.builder().vin(vin).sn(sn).build()
        );
        return poList.isEmpty() ? null : toEntity(poList.get(0));
    }

    private VehicleCcpPo toPo(VehicleCcp entity) {
        return VehicleCcpPo.builder()
            .id(entity.getId())
            .vin(entity.getVin())
            .sn(entity.getSn())
            .bindingId(entity.getBindingId())
            .partCode(entity.getPartCode())
            .vehicleNodeCode(entity.getVehicleNodeCode())
            .deviceCategory(entity.getDeviceCategory())
            .bindState(entity.getBindState())
            .bindingVersion(entity.getBindingVersion())
            .lastEventTime(entity.getLastEventTime())
            .source(entity.getSource())
            .description(entity.getDescription())
            .createBy(entity.getCreateBy())
            .modifyBy(entity.getModifyBy())
            .createTime(entity.getCreateTime())
            .build();
    }

    private VehicleCcp toEntity(VehicleCcpPo po) {
        return VehicleCcp.builder()
            .id(po.getId())
            .vin(po.getVin())
            .sn(po.getSn())
            .bindingId(po.getBindingId())
            .partCode(po.getPartCode())
            .vehicleNodeCode(po.getVehicleNodeCode())
            .deviceCategory(po.getDeviceCategory())
            .bindState(po.getBindState())
            .bindingVersion(po.getBindingVersion())
            .lastEventTime(po.getLastEventTime())
            .source(po.getSource())
            .description(po.getDescription())
            .createBy(po.getCreateBy())
            .modifyBy(po.getModifyBy())
            .createTime(po.getCreateTime())
            .build();
    }
}
