package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.IdcmVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportIdcmRequest;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.IdcmExServiceAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.IdcmInfoAppService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 信息娱乐模块信息相关服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/idcm/v1")
public class ServiceIdcmInfoController {

    private final IdcmInfoAppService idcmInfoAppService;

    /**
     * 批量导入信息娱乐模块数据
     *
     * @param request 批量导入信息娱乐模块请求
     */
    @PostMapping("/batchImport")
    public void batchImport(@RequestBody @Validated BatchImportIdcmRequest request) {
        log.info("批量导入信息娱乐模块数据[{}]", request.getBatchNum());
        idcmInfoAppService.batchImport(request.getBatchNum(), request.getSupplierCode(),
                IdcmExServiceAssembler.INSTANCE.toPoList(request.getIdcmList()));
    }

    /**
     * 根据序列号获取信息娱乐模块信息
     *
     * @param sn 序列号
     * @return 信息娱乐模块信息
     */
    @GetMapping("/{sn}")
    public IdcmVo getBySn(@PathVariable String sn) {
        log.info("根据序列号[{}]获取信息娱乐模块信息", sn);
        return IdcmExServiceAssembler.INSTANCE.fromPo(idcmInfoAppService.getBySn(sn));
    }

}
