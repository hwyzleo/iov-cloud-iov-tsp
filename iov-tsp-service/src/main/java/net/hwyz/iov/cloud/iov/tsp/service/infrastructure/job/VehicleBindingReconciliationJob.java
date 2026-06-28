package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.VehicleBindingProjectionAppService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 车辆绑定对账任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VehicleBindingReconciliationJob {

    private final VehicleBindingProjectionAppService projectionAppService;

    /**
     * 启动时执行 bootstrap 对账
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("应用启动，开始执行车辆绑定 bootstrap 对账");
        try {
            projectionAppService.bootstrap();
            log.info("车辆绑定 bootstrap 对账完成");
        } catch (Exception e) {
            log.error("车辆绑定 bootstrap 对账失败", e);
        }
    }

    /**
     * 定时对账（每天凌晨2点执行）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduledReconciliation() {
        log.info("开始执行定时车辆绑定对账");
        try {
            projectionAppService.reconcile();
            log.info("定时车辆绑定对账完成");
        } catch (Exception e) {
            log.error("定时车辆绑定对账失败", e);
        }
    }
}
