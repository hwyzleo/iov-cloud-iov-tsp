package net.hwyz.iov.cloud.tsp.mno.service.facade.assembler;

import net.hwyz.iov.cloud.tsp.mno.api.contract.SimMpt;
import net.hwyz.iov.cloud.tsp.mno.service.infrastructure.repository.po.SimPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 管理后台SIM卡转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface SimMptAssembler {

    SimMptAssembler INSTANCE = Mappers.getMapper(SimMptAssembler.class);

    /**
     * 数据对象转数据传输对象
     *
     * @param simPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    SimMpt fromPo(SimPo simPo);

    /**
     * 数据传输对象转数据对象
     *
     * @param simMpt 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    SimPo toPo(SimMpt simMpt);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param simPoList 数据对象列表
     * @return 数据传输对象列表
     */
    List<SimMpt> fromPoList(List<SimPo> simPoList);

}
