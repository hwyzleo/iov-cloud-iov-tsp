package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.CcpVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportCcpRequest;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.CcpVoAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.BatchImportCcpCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.CcpResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.CcpInfoAppService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/ccp/v1")
public class ServiceCcpInfoController {

    private final CcpInfoAppService ccpInfoAppService;

    @PostMapping("/batchImport")
    public void batchImport(@RequestBody @Validated BatchImportCcpRequest request) {
        log.info("批量导入中央计算平台数据[{}]", request.getBatchNum());
        BatchImportCcpCmd cmd = BatchImportCcpCmd.builder()
                .batchNum(request.getBatchNum())
                .supplierCode(request.getSupplierCode())
                .ccpList(CcpVoAssembler.INSTANCE.toImportCmdList(request.getCcpList()))
                .build();
        ccpInfoAppService.batchImport(cmd);
    }

    @GetMapping("/{sn}")
    public CcpVo getBySn(@PathVariable String sn) {
        log.info("根据序列号[{}]获取中央计算平台信息", sn);
        CcpResult result = ccpInfoAppService.getBySn(sn);
        return CcpVoAssembler.INSTANCE.toVo(result);
    }

}