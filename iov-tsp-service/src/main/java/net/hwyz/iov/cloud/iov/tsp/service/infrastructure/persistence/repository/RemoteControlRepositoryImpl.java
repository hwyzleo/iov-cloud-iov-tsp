package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.AbstractRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.RemoteControl;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.RemoteControlRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.cache.CacheService;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.CmdRecordPoConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.CmdRecordMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 远控领域仓库接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RemoteControlRepositoryImpl extends AbstractRepository<String, RemoteControl> implements RemoteControlRepository {

    private final CacheService cacheService;
    private final CmdRecordMapper cmdRecordMapper;

    @Override
    public RemoteControl getOrCreate(RemoteControl remoteControl) {
        RemoteControl remoteControlDoLoad = cacheService.getRemoteControl(remoteControl.getVin(), remoteControl.getType()).orElse(remoteControl);
        remoteControlDoLoad.stateLoad();
        return remoteControlDoLoad;
    }

    @Override
    public Optional<RemoteControl> getById(String s) {
        return Optional.empty();
    }

    @Override
    public boolean save(RemoteControl remoteControl) {
        switch (remoteControl.getState()) {
            case CHANGED -> {
                cmdRecordMapper.insertPo(CmdRecordPoConverter.INSTANCE.fromRemoteControlDo(remoteControl));
                cacheService.setRemoteControl(remoteControl);
            }
            default -> {
                return false;
            }
        }
        return true;
    }

}
