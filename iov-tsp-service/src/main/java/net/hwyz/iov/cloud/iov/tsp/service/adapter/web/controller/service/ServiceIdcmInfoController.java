package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.IdcmVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportIdcmRequest;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.IdcmVoAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.BatchImportIdcmCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.IdcmImportCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.IdcmResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.IdcmInfoAppService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/service/idcm/v1")
public class ServiceIdcmInfoController {

    private final IdcmInfoAppService idcmInfoAppService;

    @PostMapping("/batchImport")
    public void batchImport(@RequestBody @Validated BatchImportIdcmRequest request) {
        log.info("批量导入信息娱乐模块数据[{}]", request.getBatchNum());
        List<IdcmImportCmd> idcmImportCmdList = IdcmVoAssembler.INSTANCE.toImportCmdList(request.getIdcmList());
        BatchImportIdcmCmd cmd = BatchImportIdcmCmd.builder()
                .batchNum(request.getBatchNum())
                .supplierCode(request.getSupplierCode())
                .idcmList(idcmImportCmdList)
                .build();
        idcmInfoAppService.batchImport(cmd);
    }

    @GetMapping("/{sn}")
    public IdcmVo getBySn(@PathVariable String sn) {
        log.info("根据序列号[{}]获取信息娱乐模块信息", sn);
        IdcmResult result = idcmInfoAppService.getBySn(sn);
        return IdcmVoAssembler.INSTANCE.toVo(result);
    }

}