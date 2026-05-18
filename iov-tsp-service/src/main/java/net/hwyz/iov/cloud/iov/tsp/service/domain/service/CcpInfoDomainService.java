package net.hwyz.iov.cloud.iov.tsp.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Ccp;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.CcpLog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.CcpLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.CcpRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CcpInfoDomainService {

    private final CcpRepository ccpRepository;
    private final CcpLogRepository ccpLogRepository;

    public Ccp getBySn(String sn) {
        return ccpRepository.getBySn(sn);
    }

    public int batchImport(String batchNum, String supplierCode, List<Ccp> ccpList) {
        if (StrUtil.isBlank(supplierCode)) {
            log.warn("数据批次[{}]中央计算平台信息供应商编码为空", batchNum);
        }
        int count = 0;
        for (Ccp ccp : ccpList) {
            Ccp existingCcp = getBySn(ccp.getSn());
            if (existingCcp == null) {
                ccp.setSupplierCode(supplierCode);
                ccpRepository.save(ccp);
                recordLog(ccp, "数据批次[" + batchNum + "]数据导入");
                count++;
            } else {
                log.warn("数据批次[{}]中央计算平台信息[{}]已存在", batchNum, ccp.getSn());
            }
        }
        return count;
    }

    private void recordLog(Ccp ccp, String remark) {
        CcpLog ccpLog = CcpLog.builder()
                .sn(ccp.getSn())
                .configWord(ccp.getConfigWord())
                .hardwareVer(ccp.getHardwareVer())
                .softwareVer(ccp.getSoftwareVer())
                .hardwareNo(ccp.getHardwareNo())
                .softwareNo(ccp.getSoftwareNo())
                .hsm(ccp.getHsm())
                .description(remark)
                .build();
        ccpLogRepository.save(ccpLog);
    }

}