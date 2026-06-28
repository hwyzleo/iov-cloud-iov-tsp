package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.VehicleTbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.VehicleTboxProjectionRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.VehicleTboxConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.VehicleTboxMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleTboxPo;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class VehicleTboxProjectionRepositoryImpl implements VehicleTboxProjectionRepository {

    private final VehicleTboxMapper vehicleTboxMapper;
    private final VehicleTboxConverter vehicleTboxConverter;

    @Override
    public int upsertByBindingId(VehicleTbox vehicleTbox) {
        VehicleTboxPo po = vehicleTboxConverter.toPo(vehicleTbox);
        return vehicleTboxMapper.upsertByBindingId(po);
    }

    @Override
    public VehicleTbox getByBindingId(Long bindingId) {
        VehicleTboxPo po = vehicleTboxMapper.selectByBindingId(bindingId);
        return po != null ? vehicleTboxConverter.toEntity(po) : null;
    }

    @Override
    public List<VehicleTbox> getAllActiveBindings() {
        List<VehicleTboxPo> poList = vehicleTboxMapper.selectAllActiveBindings();
        return vehicleTboxConverter.toEntityList(poList);
    }

    @Override
    public VehicleTbox getByVin(String vin) {
        List<VehicleTboxPo> poList = vehicleTboxMapper.selectPoByExample(
            VehicleTboxPo.builder().vin(vin).bindState("ACTIVE").build()
        );
        if (poList.isEmpty()) {
            return null;
        }
        return vehicleTboxConverter.toEntity(poList.get(0));
    }

    @Override
    public VehicleTbox getByVinAndSn(String vin, String sn) {
        List<VehicleTboxPo> poList = vehicleTboxMapper.selectPoByExample(
            VehicleTboxPo.builder().vin(vin).sn(sn).build()
        );
        if (poList.isEmpty()) {
            return null;
        }
        return vehicleTboxConverter.toEntity(poList.get(0));
    }
}
