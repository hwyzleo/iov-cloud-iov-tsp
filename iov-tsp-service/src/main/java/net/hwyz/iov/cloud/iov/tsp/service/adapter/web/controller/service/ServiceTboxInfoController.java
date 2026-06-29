package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.TboxVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportTboxRequest;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.TboxVoAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.BatchImportTboxCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.TboxInfoAppService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/tbox/v1")
public class ServiceTboxInfoController {

    private final TboxInfoAppService tboxInfoAppService;

    @PostMapping("/batchImport")
    public void batchImport(@RequestBody @Validated BatchImportTboxRequest request) {
        log.info("批量导入车联终端数据[{}]", request.getBatchNum());
        BatchImportTboxCmd cmd = BatchImportTboxCmd.builder()
                .batchNum(request.getBatchNum())
                .supplierCode(request.getSupplierCode())
                .tboxList(TboxVoAssembler.INSTANCE.toImportCmdList(request.getTboxList()))
                .build();
        tboxInfoAppService.batchImport(cmd);
    }

    @GetMapping("/{sn}")
    public TboxVo getBySn(@PathVariable String sn) {
        log.info("根据序列号[{}]获取车联终端信息", sn);
        TboxResult result = tboxInfoAppService.getBySn(sn);
        if (result == null) {
            return null;
        }
        return TboxVoAssembler.INSTANCE.toVo(result);
    }

}