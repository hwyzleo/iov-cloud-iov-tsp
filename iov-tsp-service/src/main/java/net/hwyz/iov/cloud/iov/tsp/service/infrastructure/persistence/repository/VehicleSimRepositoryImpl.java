package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleSim;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleSimRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.VehicleSimConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleSimMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleSimPo;
import java.util.List;
import java.util.Optional;

/**
 * 车辆SIM卡仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class VehicleSimRepositoryImpl implements VehicleSimRepository {
    
    private final VehicleSimMapper vehicleSimMapper;
    private final VehicleSimConverter vehicleSimConverter;
    
    @Override
    public Optional<VehicleSim> getById(Long id) {
        return Optional.ofNullable(vehicleSimMapper.selectById(id))
                .map(vehicleSimConverter::toEntity);
    }
    
    @Override
    public List<VehicleSim> getByVin(String vin) {
        return vehicleSimMapper.selectByVin(vin).stream()
                .map(vehicleSimConverter::toEntity)
                .toList();
    }
    
    @Override
    public Optional<VehicleSim> getByIccid(String iccid) {
        return vehicleSimMapper.selectByIccid(iccid)
                .map(vehicleSimConverter::toEntity);
    }
    
    @Override
    public VehicleSim save(VehicleSim vehicleSim) {
        VehicleSimPo po = vehicleSimConverter.toPo(vehicleSim);
        vehicleSimMapper.insert(po);
        vehicleSim.setId(po.getId());
        return vehicleSim;
    }
    
    @Override
    public void update(VehicleSim vehicleSim) {
        VehicleSimPo po = vehicleSimConverter.toPo(vehicleSim);
        vehicleSimMapper.updateById(po);
    }
    
    @Override
    public boolean updateByIccidWithVersion(String iccid, Integer simStatus, Integer realnameStatus, Long version) {
        int rows = vehicleSimMapper.updateByIccidWithVersion(iccid, simStatus, realnameStatus, version);
        return rows > 0;
    }
    
    @Override
    public void updateIccidByVinAndSlot(String vin, Integer slotNo, String iccid) {
        vehicleSimMapper.updateIccidByVinAndSlot(vin, slotNo, iccid);
    }
}