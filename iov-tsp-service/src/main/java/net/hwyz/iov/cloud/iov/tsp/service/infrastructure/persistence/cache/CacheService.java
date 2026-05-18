package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.cache;


import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.RemoteControlType;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.RemoteControl;

import java.util.Optional;

/**
 * 缓存服务接口
 *
 * @author hwyz_leo
 */
public interface CacheService {

    /**
     * 获取远控领域对象
     *
     * @param vin               车架号
     * @param remoteControlType 远控类型
     * @return 远控领域对象
     */
    Optional<RemoteControl> getRemoteControl(String vin, RemoteControlType remoteControlType);

    /**
     * 设置远控领域对象
     *
     * @param remoteControl 远控领域对象
     */
    void setRemoteControl(RemoteControl remoteControl);

    /**
     * 删除远控领域对象
     *
     * @param cmdId 指令ID
     */
    void removeRemoteControl(String cmdId);

}
