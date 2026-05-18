package net.hwyz.iov.cloud.iov.tsp.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.RemoteControl;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.RemoteControlRepository;
import org.springframework.stereotype.Service;

/**
 * TBOX领域服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TboxDomainService {

    private final RemoteControlRepository remoteControlRepository;

    public RemoteControl getOrCreateRemoteControl(RemoteControl remoteControl) {
        return remoteControlRepository.getOrCreate(remoteControl);
    }

}
