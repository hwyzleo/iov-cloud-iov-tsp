package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.util.StrUtil;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.TboxLogMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxLogPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxPo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 车联终端信息相关应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TboxInfoAppService {

    private final TboxMapper tboxMapper;
    private final TboxLogMapper tboxLogMapper;

    /**
     * 批量导入车联终端信息
     *
     * @param batchNum     批次号
     * @param supplierCode 供应商编码
     * @param tboxList     车联终端列表
     */
    public void batchImport(String batchNum, String supplierCode, List<TboxPo> tboxList) {
        if (StrUtil.isBlank(supplierCode)) {
            log.warn("数据批次[{}]车联终端信息供应商编码为空", batchNum);
        }
        for (TboxPo tboxPo : tboxList) {
            if (ObjUtil.isNull(tboxMapper.selectBySn(tboxPo.getSn()))) {
                tboxPo.setSupplierCode(supplierCode);
                tboxMapper.insertPo(tboxPo);
                recordLog(tboxPo, "数据批次[" + batchNum + "]数据导入");
            } else {
                log.warn("数据批次[{}]车联终端信息[{}]已存在", batchNum, tboxPo.getSn());
            }
        }
    }

    /**
     * 根据序列号获取车联终端信息
     *
     * @param sn 序列号
     * @return 车联终端信息
     */
    public TboxPo getBySn(String sn) {
        return tboxMapper.selectBySn(sn);
    }

    /**
     * 记录车联终端信息变更日志
     *
     * @param tboxPo 车联终端对象
     * @param remark 变更备注
     */
    private void recordLog(TboxPo tboxPo, String remark) {
        tboxLogMapper.insertPo(TboxLogPo.builder()
                .sn(tboxPo.getSn())
                .configWord(tboxPo.getConfigWord())
                .hardwareVer(tboxPo.getHardwareVer())
                .softwareVer(tboxPo.getSoftwareVer())
                .hardwareNo(tboxPo.getHardwareNo())
                .softwareNo(tboxPo.getSoftwareNo())
                .hsm(tboxPo.getHsm())
                .iccid1(tboxPo.getIccid1())
                .iccid2(tboxPo.getIccid2())
                .description(remark)
                .build());
    }

}
