package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 车辆绑定投影应用服务
 * <p>
 * 负责将绑定事件投影到读模型，并执行对账逻辑。
 * 完整实现将在 Task 4 中完成。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleBindingProjectionAppService {

    /**
     * 启动时 bootstrap：从绑定表全量加载投影
     */
    public void bootstrap() {
        // TODO: Task 4 完整实现
        log.debug("VehicleBindingProjectionAppService.bootstrap - placeholder");
    }

    /**
     * 定时对账：与绑定提供方比对并修复不一致
     */
    public void reconcile() {
        // TODO: Task 4 完整实现
        log.debug("VehicleBindingProjectionAppService.reconcile - placeholder");
    }
}
