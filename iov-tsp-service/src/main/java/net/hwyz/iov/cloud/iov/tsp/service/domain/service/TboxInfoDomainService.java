package net.hwyz.iov.cloud.iov.tsp.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Tbox;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.TboxLog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TboxInfoDomainService {

    private final TboxRepository tboxRepository;
    private final TboxLogRepository tboxLogRepository;

    public Tbox getBySn(String sn) {
        return tboxRepository.getBySn(sn);
    }

    public int batchImport(String batchNum, String supplierCode, List<Tbox> tboxList) {
        if (StrUtil.isBlank(supplierCode)) {
            log.warn("数据批次[{}]车联终端信息供应商编码为空", batchNum);
        }
        int count = 0;
        for (Tbox tbox : tboxList) {
            Tbox existingTbox = getBySn(tbox.getSn());
            if (existingTbox == null) {
                tbox.setSupplierCode(supplierCode);
                tboxRepository.save(tbox);
                recordLog(tbox, "数据批次[" + batchNum + "]数据导入");
                count++;
            } else {
                log.warn("数据批次[{}]车联终端信息[{}]已存在", batchNum, tbox.getSn());
            }
        }
        return count;
    }

    private void recordLog(Tbox tbox, String remark) {
        TboxLog tboxLog = TboxLog.builder()
                .sn(tbox.getSn())
                .configWord(tbox.getConfigWord())
                .hardwareVer(tbox.getHardwareVer())
                .softwareVer(tbox.getSoftwareVer())
                .hardwareNo(tbox.getHardwareNo())
                .softwareNo(tbox.getSoftwareNo())
                .hsm(tbox.getHsm())
                .iccid1(tbox.getIccid1())
                .iccid2(tbox.getIccid2())
                .imei(tbox.getImei())
                .description(remark)
                .build();
        tboxLogRepository.save(tboxLog);
    }

}