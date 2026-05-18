package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.framework.common.domain.BaseRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.RemoteControl;

/**
 * 远控领域仓库接口
 *
 * @author hwyz_leo
 */
public interface RemoteControlRepository extends BaseRepository<String, RemoteControl> {

    /**
     * 获取或新建远控领域对象
     *
     * @param remoteControlDo 远控领域对象
     * @return 远控领域对象
     */
    RemoteControl getOrCreate(RemoteControl remoteControlDo);

}
