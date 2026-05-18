package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.BatchImportSimRequest;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.SimAppService;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.SimExServiceAssembler;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * SIM卡相关服务接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/service/sim")
public class ServiceSimController {

    private final SimAppService simAppService;

    /**
     * 批量导入SIM卡信息
     *
     * @param request 批量导入SIM卡信息请求
     */
    @PostMapping("/batchImport")
    public void batchImport(@RequestBody @Validated BatchImportSimRequest request) {
        log.info("批量导入运营商[{}]SIM卡信息[{}]", request.getMnoType(), request.getSimList().size());
        simAppService.batchImport(request.getBatchNum(), request.getMnoType(), SimExServiceAssembler.INSTANCE.toPoList(request.getSimList()));
    }

}
