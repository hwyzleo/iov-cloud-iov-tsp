package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.IdcmLogMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.IdcmMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.IdcmLogPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.IdcmPo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 信息娱乐模块信息相关应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IdcmInfoAppService {

    private final IdcmMapper idcmDao;
    private final IdcmLogMapper idcmLogDao;

    /**
     * 批量导入信息娱乐模块信息
     *
     * @param batchNum     批次号
     * @param supplierCode 供应商编码
     * @param idcmList     信息娱乐模块列表
     */
    public void batchImport(String batchNum, String supplierCode, List<IdcmPo> idcmList) {
        if (StrUtil.isBlank(supplierCode)) {
            log.warn("数据批次[{}]信息娱乐模块信息供应商编码为空", batchNum);
        }
        for (IdcmPo idcmPo : idcmList) {
            if (ObjUtil.isNull(idcmDao.selectBySn(idcmPo.getSn()))) {
                idcmPo.setSupplierCode(supplierCode);
                idcmDao.insertPo(idcmPo);
                recordLog(idcmPo, "数据批次[" + batchNum + "]数据导入");
            } else {
                log.warn("数据批次[{}]信息娱乐模块信息[{}]已存在", batchNum, idcmPo.getSn());
            }
        }
    }

    /**
     * 根据序列号获取信息娱乐模块信息
     *
     * @param sn 序列号
     * @return 信息娱乐模块信息
     */
    public IdcmPo getBySn(String sn) {
        return idcmDao.selectBySn(sn);
    }

    /**
     * 记录信息娱乐模块信息变更日志
     *
     * @param idcmPo 信息娱乐模块对象
     * @param remark 变更备注
     */
    private void recordLog(IdcmPo idcmPo, String remark) {
        idcmLogDao.insertPo(IdcmLogPo.builder()
                .sn(idcmPo.getSn())
                .configWord(idcmPo.getConfigWord())
                .hardwareVer(idcmPo.getHardwareVer())
                .softwareVer(idcmPo.getSoftwareVer())
                .hardwareNo(idcmPo.getHardwareNo())
                .softwareNo(idcmPo.getSoftwareNo())
                .hsm(idcmPo.getHsm())
                .mac(idcmPo.getMac())
                .description(remark)
                .build());
    }

}
