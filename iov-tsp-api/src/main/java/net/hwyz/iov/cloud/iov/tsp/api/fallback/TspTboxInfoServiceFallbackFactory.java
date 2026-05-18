package net.hwyz.iov.cloud.iov.tsp.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspTboxInfoService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.TboxVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportTboxRequest;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 车联终端信息相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class TspTboxInfoServiceFallbackFactory implements FallbackFactory<TspTboxInfoService> {

    @Override
    public TspTboxInfoService create(Throwable throwable) {
        return new TspTboxInfoService() {
            @Override
            public void batchImport(BatchImportTboxRequest request) {
                log.error("车联终端信息相关服务批量导入车联终端数据[{}]调用异常", request.getBatchNum(), throwable);
            }

            @Override
            public TboxVo getBySn(String sn) {
                log.error("车联终端信息相关服务根据序列号[{}]获取车联终端信息调用异常", sn, throwable);
                return null;
            }
        };
    }
}
