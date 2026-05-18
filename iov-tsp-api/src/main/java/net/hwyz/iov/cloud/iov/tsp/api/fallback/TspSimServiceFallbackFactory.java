package net.hwyz.iov.cloud.iov.tsp.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportSimRequest;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspSimService;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * SIM卡相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class TspSimServiceFallbackFactory implements FallbackFactory<TspSimService> {

    @Override
    public TspSimService create(Throwable throwable) {
        return new TspSimService() {
            @Override
            public void batchImport(BatchImportSimRequest request) {
                log.error("通讯运营商服务批量导入运营商[{}]SIM卡信息[{}]调用失败", request.getMnoType(), request.getSimList().size(), throwable);
            }
        };
    }
}
