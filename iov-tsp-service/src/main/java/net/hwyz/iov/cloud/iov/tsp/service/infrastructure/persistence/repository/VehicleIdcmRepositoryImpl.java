package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleIdcm;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleIdcmRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.VehicleIdcmConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleIdcmMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleIdcmPo;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class VehicleIdcmRepositoryImpl implements VehicleIdcmRepository {

    private final VehicleIdcmMapper vehicleIdcmMapper;
    private final VehicleIdcmConverter vehicleIdcmConverter;

    @Override
    public VehicleIdcm getByVin(String vin) {
        List<VehicleIdcmPo> poList = vehicleIdcmMapper.selectPoByExample(
                VehicleIdcmPo.builder().vin(vin).build()
        );
        return poList.isEmpty() ? null : vehicleIdcmConverter.toEntity(poList.get(0));
    }

    @Override
    public VehicleIdcm getBySn(String sn) {
        List<VehicleIdcmPo> poList = vehicleIdcmMapper.selectPoByExample(
                VehicleIdcmPo.builder().sn(sn).build()
        );
        return poList.isEmpty() ? null : vehicleIdcmConverter.toEntity(poList.get(0));
    }

    @Override
    public int save(VehicleIdcm vehicleIdcm) {
        VehicleIdcmPo po = vehicleIdcmConverter.toPo(vehicleIdcm);
        if (po.getId() == null) {
            return vehicleIdcmMapper.insertPo(po);
        } else {
            return vehicleIdcmMapper.updatePo(po);
        }
    }

}