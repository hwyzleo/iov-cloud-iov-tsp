package net.hwyz.iov.cloud.iov.tsp.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.iov.tsp.api.fallback.TspCcpInfoServiceFallbackFactory;
import net.hwyz.iov.cloud.iov.tsp.api.vo.CcpVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportCcpRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 中央计算平台信息相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "tspCcpInfoService", value = ServiceNameConstants.IOV_TSP, path = "/api/service/ccp/v1", fallbackFactory = TspCcpInfoServiceFallbackFactory.class)
public interface TspCcpInfoService {

    /**
     * 批量导入中央计算平台数据
     *
     * @param request 批量导入中央计算平台请求
     */
    @PostMapping("/batchImport")
    void batchImport(@RequestBody @Validated BatchImportCcpRequest request);

    /**
     * 根据序列号获取中央计算平台信息
     *
     * @param sn 序列号
     * @return 中央计算平台信息
     */
    @GetMapping("/{sn}")
    CcpVo getBySn(@PathVariable String sn);

}
