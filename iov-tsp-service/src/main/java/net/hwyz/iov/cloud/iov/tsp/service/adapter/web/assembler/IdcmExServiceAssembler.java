package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.IdcmVo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.IdcmPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务信息娱乐模块信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface IdcmExServiceAssembler {

    IdcmExServiceAssembler INSTANCE = Mappers.getMapper(IdcmExServiceAssembler.class);

    /**
     * 数据传输对象转数据对象
     *
     * @param idcmExService 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    IdcmPo toPo(IdcmVo idcmExService);

    /**
     * 数据对象转数据传输对象
     *
     * @param idcmPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    IdcmVo fromPo(IdcmPo idcmPo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param idcmExServiceList 数据传输对象列表
     * @return 数据对象列表
     */
    List<IdcmPo> toPoList(List<IdcmVo> idcmExServiceList);

}
