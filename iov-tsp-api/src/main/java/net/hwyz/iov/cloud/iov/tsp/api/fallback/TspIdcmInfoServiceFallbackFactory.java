package net.hwyz.iov.cloud.iov.tsp.api.fallback;

import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.service.TspIdcmInfoService;
import net.hwyz.iov.cloud.iov.tsp.api.vo.IdcmVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportIdcmRequest;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

/**
 * 信息娱乐模块信息相关服务降级处理
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class TspIdcmInfoServiceFallbackFactory implements FallbackFactory<TspIdcmInfoService> {

    @Override
    public TspIdcmInfoService create(Throwable throwable) {
        return new TspIdcmInfoService() {
            @Override
            public void batchImport(BatchImportIdcmRequest request) {
                log.error("信息娱乐模块信息相关服务批量导入信息娱乐模块数据[{}]调用异常", request.getBatchNum(), throwable);
            }

            @Override
            public IdcmVo getBySn(String sn) {
                log.error("信息娱乐模块信息相关服务根据序列号[{}]获取信息娱乐模块信息调用异常", sn, throwable);
                return null;
            }
        };
    }
}
