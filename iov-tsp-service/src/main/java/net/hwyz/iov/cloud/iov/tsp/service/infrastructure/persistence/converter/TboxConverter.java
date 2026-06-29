package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Tbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.TboxLog;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxLogPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxPo;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TboxConverter {

    TboxPo toPo(Tbox tbox);

    Tbox toEntity(TboxPo po);

    List<Tbox> toEntityList(List<TboxPo> poList);

    /**
     * TboxPo 转 TboxResult
     *
     * @param tboxPo TBOX 持久化对象
     * @return TBOX 结果
     */
    TboxResult toResult(TboxPo tboxPo);

    TboxLogPo toLogPo(TboxLog tboxLog);

}