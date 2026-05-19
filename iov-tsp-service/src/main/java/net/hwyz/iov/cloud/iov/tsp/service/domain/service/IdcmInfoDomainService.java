package net.hwyz.iov.cloud.iov.tsp.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Idcm;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.IdcmLog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.IdcmLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.IdcmRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdcmInfoDomainService {

    private final IdcmRepository idcmRepository;
    private final IdcmLogRepository idcmLogRepository;

    public Idcm getBySn(String sn) {
        return idcmRepository.getBySn(sn);
    }

    @Transactional(rollbackFor = Exception.class)
    public int batchImport(String batchNum, String supplierCode, List<Idcm> idcmList) {
        if (StrUtil.isBlank(supplierCode)) {
            log.warn("数据批次[{}]信息娱乐模块信息供应商编码为空", batchNum);
        }
        int count = 0;
        for (Idcm idcm : idcmList) {
            Idcm existingIdcm = getBySn(idcm.getSn());
            if (existingIdcm == null) {
                idcm.setSupplierCode(supplierCode);
                idcmRepository.save(idcm);
                recordLog(idcm, "数据批次[" + batchNum + "]数据导入");
                count++;
            } else {
                log.warn("数据批次[{}]信息娱乐模块信息[{}]已存在", batchNum, idcm.getSn());
            }
        }
        return count;
    }

    private void recordLog(Idcm idcm, String remark) {
        IdcmLog idcmLog = IdcmLog.builder()
                .sn(idcm.getSn())
                .configWord(idcm.getConfigWord())
                .hardwareVer(idcm.getHardwareVer())
                .softwareVer(idcm.getSoftwareVer())
                .hardwareNo(idcm.getHardwareNo())
                .softwareNo(idcm.getSoftwareNo())
                .hsm(idcm.getHsm())
                .mac(idcm.getMac())
                .description(remark)
                .build();
        idcmLogRepository.save(idcmLog);
    }

}