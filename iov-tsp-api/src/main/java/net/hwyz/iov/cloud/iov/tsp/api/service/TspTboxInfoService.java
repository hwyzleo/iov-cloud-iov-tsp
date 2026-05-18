package net.hwyz.iov.cloud.iov.tsp.api.service;

import net.hwyz.iov.cloud.framework.common.constant.ServiceNameConstants;
import net.hwyz.iov.cloud.iov.tsp.api.fallback.TspTboxInfoServiceFallbackFactory;
import net.hwyz.iov.cloud.iov.tsp.api.vo.TboxVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportTboxRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 车联终端信息相关服务接口
 *
 * @author hwyz_leo
 */
@FeignClient(contextId = "tspTboxInfoService", value = ServiceNameConstants.IOV_TSP, path = "/api/service/tbox/v1", fallbackFactory = TspTboxInfoServiceFallbackFactory.class)
public interface TspTboxInfoService {

    /**
     * 批量导入车联终端数据
     *
     * @param request 批量导入车联终端请求
     */
    @PostMapping("/batchImport")
    void batchImport(@RequestBody @Validated BatchImportTboxRequest request);

    /**
     * 根据序列号获取车联终端信息
     *
     * @param sn 序列号
     * @return 车联终端信息
     */
    @GetMapping("/{sn}")
    TboxVo getBySn(@PathVariable String sn);

}
