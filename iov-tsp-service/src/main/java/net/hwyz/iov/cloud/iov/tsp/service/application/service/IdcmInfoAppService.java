package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.assembler.IdcmAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.BatchImportIdcmCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.IdcmResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Idcm;
import net.hwyz.iov.cloud.iov.tsp.service.domain.service.IdcmInfoDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdcmInfoAppService {

    private final IdcmInfoDomainService idcmInfoDomainService;

    @Transactional(rollbackFor = Exception.class)
    public int batchImport(BatchImportIdcmCmd cmd) {
        List<Idcm> idcmList = IdcmAssembler.INSTANCE.toEntityList(cmd.getIdcmList());
        return idcmInfoDomainService.batchImport(cmd.getBatchNum(), cmd.getSupplierCode(), idcmList);
    }

    public IdcmResult getBySn(String sn) {
        Idcm idcm = idcmInfoDomainService.getBySn(sn);
        return IdcmAssembler.INSTANCE.toResult(idcm);
    }

}