package net.hwyz.iov.cloud.iov.tsp.service.domain.service;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.service.domain.enums.SimState;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Sim;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.SimLog;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.SimLogRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.SimRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SimDomainService {

    private final SimRepository simRepository;
    private final SimLogRepository simLogRepository;

    public Sim getById(Long id) {
        return simRepository.getById(id);
    }

    public Sim getByIccid(String iccid) {
        return simRepository.getByIccid(iccid);
    }

    public List<Sim> search(Map<String, Object> conditions) {
        return simRepository.search(conditions);
    }

    public boolean checkIccidUnique(Long simId, String iccid) {
        Sim sim = getByIccid(iccid);
        if (sim == null) {
            return true;
        }
        return simId != null && sim.getId().equals(simId);
    }

    public int create(Sim sim) {
        int result = simRepository.save(sim);
        recordLog(sim, "管理后台新增");
        return result;
    }

    public int batchImport(String batchNum, String mnoCode, List<Sim> simList) {
        int count = 0;
        for (Sim sim : simList) {
            Sim existingSim = getByIccid(sim.getIccid());
            if (existingSim == null) {
                sim.setMnoCode(mnoCode);
                sim.setSimState(SimState.TEST);
                sim.enableAllAbilities();
                simRepository.save(sim);
                recordLog(sim, "数据批次[" + batchNum + "]数据导入");
                count++;
            }
        }
        return count;
    }

    public int update(Sim sim) {
        return simRepository.update(sim);
    }

    public int deleteByIds(Long[] ids) {
        for (Long id : ids) {
            Sim sim = getById(id);
            if (sim != null) {
                simLogRepository.deleteByIccid(sim.getIccid());
            }
        }
        return simRepository.deleteByIds(ids);
    }

    private void recordLog(Sim sim, String remark) {
        SimLog simLog = SimLog.builder()
                .iccid(sim.getIccid())
                .simState(sim.getSimState())
                .smsAbility(sim.getSmsAbility())
                .dataAbility(sim.getDataAbility())
                .voiceAbility(sim.getVoiceAbility())
                .description(remark)
                .build();
        simLogRepository.save(simLog);
    }

}