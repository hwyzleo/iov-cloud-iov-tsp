package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.assembler.CcpAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.BatchImportCcpCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.CcpResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Ccp;
import net.hwyz.iov.cloud.iov.tsp.service.domain.service.CcpInfoDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CcpInfoAppService {

    private final CcpInfoDomainService ccpInfoDomainService;

    @Transactional(rollbackFor = Exception.class)
    public int batchImport(BatchImportCcpCmd cmd) {
        List<Ccp> ccpList = CcpAssembler.INSTANCE.toEntityList(cmd.getCcpList());
        return ccpInfoDomainService.batchImport(cmd.getBatchNum(), cmd.getSupplierCode(), ccpList);
    }

    public CcpResult getBySn(String sn) {
        Ccp ccp = ccpInfoDomainService.getBySn(sn);
        return CcpAssembler.INSTANCE.toResult(ccp);
    }

}