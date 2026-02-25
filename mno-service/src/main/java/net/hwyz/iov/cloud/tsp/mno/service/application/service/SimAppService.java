package net.hwyz.iov.cloud.tsp.mno.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.tsp.mno.api.contract.enums.MnoType;
import net.hwyz.iov.cloud.tsp.mno.service.domain.contract.enums.SimState;
import net.hwyz.iov.cloud.tsp.mno.service.infrastructure.repository.dao.SimDao;
import net.hwyz.iov.cloud.tsp.mno.service.infrastructure.repository.dao.SimLogDao;
import net.hwyz.iov.cloud.tsp.mno.service.infrastructure.repository.po.SimLogPo;
import net.hwyz.iov.cloud.tsp.mno.service.infrastructure.repository.po.SimPo;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SIM卡应用服务类
 *
 * @author hwyz_leo
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimAppService {

    private final SimDao simDao;
    private final SimLogDao simLogDao;

    /**
     * 查询SIM卡信息
     *
     * @param iccid     ICCID
     * @param beginTime 开始时间
     * @param endTime   结束时间
     * @return SIM卡列表
     */
    public List<SimPo> search(String iccid, Date beginTime, Date endTime) {
        Map<String, Object> map = new HashMap<>();
        map.put("iccid", iccid);
        map.put("beginTime", beginTime);
        map.put("endTime", endTime);
        return simDao.selectPoByMap(map);
    }

    /**
     * 检查SIM卡ICCID是否唯一
     *
     * @param simId SIM卡ID
     * @param iccid ICCID
     * @return 结果
     */
    public Boolean checkIccidUnique(Long simId, String iccid) {
        if (ObjUtil.isNull(simId)) {
            simId = -1L;
        }
        SimPo simPo = getSimByIccid(iccid);
        return !ObjUtil.isNotNull(simPo) || simPo.getId().longValue() == simId.longValue();
    }

    /**
     * 根据主键ID获取SIM卡信息
     *
     * @param id 主键ID
     * @return SIM卡信息
     */
    public SimPo getSimById(Long id) {
        return simDao.selectPoById(id);
    }

    /**
     * 根据ICCID获取SIM卡信息
     *
     * @param iccid ICCID
     * @return SIM卡信息
     */
    public SimPo getSimByIccid(String iccid) {
        return simDao.selectByIccid(iccid);
    }

    /**
     * 新增SIM卡
     *
     * @param sim SIM卡信息
     * @return 结果
     */
    public int createSim(SimPo sim) {
        int result = simDao.insertPo(sim);
        recordLog(sim, "管理后台新增");
        return result;
    }

    /**
     * 批量导入SIM卡信息
     *
     * @param batchNum 批次号
     * @param mnoType  运营商类型
     * @param simList  SIM卡列表
     */
    public void batchImport(String batchNum, MnoType mnoType, List<SimPo> simList) {
        for (SimPo simPo : simList) {
            if (ObjUtil.isNull(simDao.selectByIccid(simPo.getIccid()))) {
                simPo.setMnoCode(mnoType.name());
                simPo.setSimState(SimState.TEST.state);
                simPo.setDataAbility(true);
                simPo.setSmsAbility(true);
                simPo.setVoiceAbility(true);
                simDao.insertPo(simPo);
                recordLog(simPo, "数据批次[" + batchNum + "]数据导入");
            } else {
                logger.warn("数据批次[{}]SIM卡[{}]已存在", batchNum, simPo.getIccid());
            }
        }
    }

    /**
     * 修改SIM卡
     *
     * @param sim SIM卡信息
     * @return 结果
     */
    public int modifySim(SimPo sim) {
        return simDao.updatePo(sim);
    }

    /**
     * 批量删除SIM卡
     *
     * @param ids SIM卡ID数组
     * @return 结果
     */
    public int deleteSimByIds(Long[] ids) {
        for (Long id : ids) {
            SimPo sim = getSimById(id);
            if (sim != null) {
                simLogDao.physicalDeletePoByIccid(sim.getIccid());
            }
        }
        return simDao.batchPhysicalDeletePo(ids);
    }

    /**
     * 记录SIM卡变更日志
     *
     * @param simPo  SIM卡对象
     * @param remark 变更备注
     */
    private void recordLog(SimPo simPo, String remark) {
        simLogDao.insertPo(SimLogPo.builder()
                .iccid(simPo.getIccid())
                .simState(simPo.getSimState())
                .smsAbility(simPo.getSmsAbility())
                .dataAbility(simPo.getDataAbility())
                .voiceAbility(simPo.getVoiceAbility())
                .description(remark)
                .build());
    }

}
