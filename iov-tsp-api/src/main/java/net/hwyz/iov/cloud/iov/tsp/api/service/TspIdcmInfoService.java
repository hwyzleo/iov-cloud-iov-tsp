package net.hwyz.iov.cloud.iov.tsp.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.iov.tsp.api.fallback.TspIdcmInfoServiceFallbackFactory;
import net.hwyz.iov.cloud.iov.tsp.api.vo.IdcmVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportIdcmRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 信息娱乐模块信息相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "tspIdcmInfoService", value = ServiceNameConstants.IOV_TSP, path = "/api/service/idcm/v1", fallbackFactory = TspIdcmInfoServiceFallbackFactory.class)
public interface TspIdcmInfoService {

    /**
     * 批量导入信息娱乐模块数据
     *
     * @param request 批量导入信息娱乐模块请求
     */
    @PostMapping("/batchImport")
    void batchImport(@RequestBody @Validated BatchImportIdcmRequest request);

    /**
     * 根据序列号获取信息娱乐模块信息
     *
     * @param sn 序列号
     * @return 信息娱乐模块信息
     */
    @GetMapping("/{sn}")
    IdcmVo getBySn(@PathVariable String sn);

}
