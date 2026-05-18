package net.hwyz.iov.cloud.iov.tsp.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspCcpInfoService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.CcpVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportCcpRequest;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 中央计算平台信息相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class TspCcpInfoServiceFallbackFactory implements FallbackFactory<TspCcpInfoService> {

    @Override
    public TspCcpInfoService create(Throwable throwable) {
        return new TspCcpInfoService() {
            @Override
            public void batchImport(BatchImportCcpRequest request) {
                log.error("中央计算平台信息相关服务批量导入中央计算平台数据[{}]调用异常", request.getBatchNum(), throwable);
            }

            @Override
            public CcpVo getBySn(String sn) {
                log.error("中央计算平台信息相关服务根据序列号[{}]获取中央计算平台信息调用异常", sn, throwable);
                return null;
            }
        };
    }
}
