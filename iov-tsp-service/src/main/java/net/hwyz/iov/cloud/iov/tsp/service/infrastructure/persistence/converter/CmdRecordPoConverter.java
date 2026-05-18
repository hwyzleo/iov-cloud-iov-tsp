package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.RemoteControl;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.CmdRecordPo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * 远控命令记录数据对象转换类
 *
 * @author hwyz_leo
 */
@Mapper
public interface CmdRecordPoConverter {

    CmdRecordPoConverter INSTANCE = Mappers.getMapper(CmdRecordPoConverter.class);

    /**
     * 数据对象转领域对象
     *
     * @param cmdRecordPo 数据对象
     * @return 远控领域对象
     */
    @Mappings({
            @Mapping(target = "type", source = "cmdType"),
            @Mapping(target = "params", expression = "java(net.hwyz.iov.cloud.framework.common.util.AssemblerHelper.json2Map(cmdRecordPo.getCmdParam()))"),
            @Mapping(target = "msgFlow", expression = "java(net.hwyz.iov.cloud.iov.tsp.service.common.enums.MsgFlow.valOf(cmdRecordPo.getMsgFlow()))")
    })
    RemoteControl toRemoteControlDo(CmdRecordPo cmdRecordPo);

    /**
     * 远控领域对象转数据对象
     *
     * @param remoteControl 远控领域对象
     * @return 数据对象
     */
    @Mappings({
            @Mapping(target = "cmdType", expression = "java(net.hwyz.iov.cloud.iov.tsp.service.common.enums.TboxCmdType.REMOTE_CONTROL.name())"),
            @Mapping(target = "cmdParam", expression = "java(net.hwyz.iov.cloud.framework.common.util.AssemblerHelper.map2Json(remoteControl.getParams()))"),
            @Mapping(target = "msgFlow", expression = "java(remoteControl.getMsgFlow().getValue())")
    })
    CmdRecordPo fromRemoteControlDo(RemoteControl remoteControl);

}
