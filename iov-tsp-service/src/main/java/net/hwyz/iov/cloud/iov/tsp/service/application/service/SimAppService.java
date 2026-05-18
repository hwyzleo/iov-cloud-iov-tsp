package net.hwyz.iov.cloud.iov.tsp.service.application.service;

import cn.hutool.core.util.ObjUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.iov.tsp.service.application.assembler.SimAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.BatchImportSimCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.SimCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.query.SimQuery;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.SimResult;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity.Sim;
import net.hwyz.iov.cloud.iov.tsp.service.domain.service.SimDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimAppService {

    private final SimDomainService simDomainService;

    public List<SimResult> search(SimQuery query) {
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("iccid", query.getIccid());
        conditions.put("beginTime", query.getBeginTime());
        conditions.put("endTime", query.getEndTime());
        List<Sim> simList = simDomainService.search(conditions);
        return SimAssembler.INSTANCE.toResultList(simList);
    }

    public boolean checkIccidUnique(Long simId, String iccid) {
        return simDomainService.checkIccidUnique(simId, iccid);
    }

    public SimResult getById(Long id) {
        Sim sim = simDomainService.getById(id);
        return SimAssembler.INSTANCE.toResult(sim);
    }

    @Transactional(rollbackFor = Exception.class)
    public int create(SimCmd cmd) {
        Sim sim = SimAssembler.INSTANCE.toEntity(cmd);
        return simDomainService.create(sim);
    }

    @Transactional(rollbackFor = Exception.class)
    public int batchImport(BatchImportSimCmd cmd) {
        List<Sim> simList = SimAssembler.INSTANCE.toEntityList(cmd.getSimList());
        return simDomainService.batchImport(cmd.getBatchNum(), cmd.getMnoCode(), simList);
    }

    @Transactional(rollbackFor = Exception.class)
    public int update(SimCmd cmd) {
        Sim sim = SimAssembler.INSTANCE.toEntity(cmd);
        return simDomainService.update(sim);
    }

    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(Long[] ids) {
        return simDomainService.deleteByIds(ids);
    }

}