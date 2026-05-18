package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.TboxVo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 对外服务车联终端信息转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface TboxExServiceAssembler {

    TboxExServiceAssembler INSTANCE = Mappers.getMapper(TboxExServiceAssembler.class);

    /**
     * 数据传输对象转数据对象
     *
     * @param tboxExService 数据传输对象
     * @return 数据对象
     */
    @Mappings({})
    TboxPo toPo(TboxVo tboxExService);

    /**
     * 数据对象转数据传输对象
     *
     * @param tboxPo 数据对象
     * @return 数据传输对象
     */
    @Mappings({})
    TboxVo fromPo(TboxPo tboxPo);

    /**
     * 数据对象列表转数据传输对象列表
     *
     * @param tboxExServiceList 数据传输对象列表
     * @return 数据对象列表
     */
    List<TboxPo> toPoList(List<TboxVo> tboxExServiceList);

}
