package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.assembler.TboxAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.BatchImportTboxCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.TboxResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Tbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.service.TboxInfoDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TboxInfoAppService {

    private final TboxInfoDomainService tboxInfoDomainService;

    @Transactional(rollbackFor = Exception.class)
    public int batchImport(BatchImportTboxCmd cmd) {
        List<Tbox> tboxList = TboxAssembler.INSTANCE.toEntityList(cmd.getTboxList());
        return tboxInfoDomainService.batchImport(cmd.getBatchNum(), cmd.getSupplierCode(), tboxList);
    }

    public TboxResult getBySn(String sn) {
        Tbox tbox = tboxInfoDomainService.getBySn(sn);
        return TboxAssembler.INSTANCE.toResult(tbox);
    }

}