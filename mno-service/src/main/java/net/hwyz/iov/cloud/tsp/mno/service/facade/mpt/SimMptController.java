package net.hwyz.iov.cloud.tsp.mno.service.facade.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.common.web.domain.AjaxResult;
import net.hwyz.iov.cloud.framework.common.web.page.TableDataInfo;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.tsp.mno.api.contract.SimMpt;
import net.hwyz.iov.cloud.tsp.mno.api.feign.mpt.SimMptApi;
import net.hwyz.iov.cloud.tsp.mno.service.application.service.SimAppService;
import net.hwyz.iov.cloud.tsp.mno.service.facade.assembler.SimMptAssembler;
import net.hwyz.iov.cloud.tsp.mno.service.infrastructure.repository.po.SimPo;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * SIM卡相关管理接口实现类
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/mpt/sim")
public class SimMptController extends BaseController implements SimMptApi {

    private final SimAppService simAppService;

    /**
     * 分页查询SIM卡信息
     *
     * @param sim SIM卡信息
     * @return SIM卡信息列表
     */
    @RequiresPermissions("iov:mno:sim:list")
    @Override
    @GetMapping(value = "/list")
    public TableDataInfo list(SimMpt sim) {
        logger.info("管理后台用户[{}]分页查询SIM卡信息", SecurityUtils.getUsername());
        startPage();
        List<SimPo> simPoList = simAppService.search(sim.getIccid(), getBeginTime(sim),
                getEndTime(sim));
        List<SimMpt> simMptList = SimMptAssembler.INSTANCE.fromPoList(simPoList);
        return getDataTable(simPoList, simMptList);
    }

    /**
     * 导出SIM卡信息
     *
     * @param response 响应
     * @param sim      SIM卡信息
     */
    @Log(title = "SIM卡管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("iov:mno:sim:export")
    @Override
    @PostMapping("/export")
    public void export(HttpServletResponse response, SimMpt sim) {
        logger.info("管理后台用户[{}]导出车辆品牌信息", SecurityUtils.getUsername());
    }

    /**
     * 根据SIM卡ID获取SIM卡信息
     *
     * @param simId SIM卡ID
     * @return SIM卡信息
     */
    @RequiresPermissions("iov:mno:sim:query")
    @Override
    @GetMapping(value = "/{simId}")
    public AjaxResult getInfo(@PathVariable Long simId) {
        logger.info("管理后台用户[{}]根据SIM卡ID[{}]获取SIM卡信息", SecurityUtils.getUsername(), simId);
        SimPo simPo = simAppService.getSimById(simId);
        return success(SimMptAssembler.INSTANCE.fromPo(simPo));
    }

    /**
     * 新增SIM卡信息
     *
     * @param sim SIM卡信息
     * @return 结果
     */
    @Log(title = "SIM卡管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("iov:mno:sim:add")
    @Override
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SimMpt sim) {
        logger.info("管理后台用户[{}]新增SIM卡信息[{}]", SecurityUtils.getUsername(), sim.getIccid());
        if (!simAppService.checkIccidUnique(sim.getId(), sim.getIccid())) {
            return error("新增SIM卡'" + sim.getIccid() + "'失败，ICCID已存在");
        }
        SimPo simPo = SimMptAssembler.INSTANCE.toPo(sim);
        simPo.setCreateBy(SecurityUtils.getUserId().toString());
        return toAjax(simAppService.createSim(simPo));
    }

    /**
     * 修改保存SIM卡信息
     *
     * @param sim SIM卡信息
     * @return 结果
     */
    @Log(title = "SIM卡管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("iov:mno:sim:edit")
    @Override
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SimMpt sim) {
        logger.info("管理后台用户[{}]修改保存SIM卡信息[{}]", SecurityUtils.getUsername(), sim.getIccid());
        if (!simAppService.checkIccidUnique(sim.getId(), sim.getIccid())) {
            return error("修改保存SIM卡'" + sim.getIccid() + "'失败，ICCID已存在");
        }
        SimPo simPo = SimMptAssembler.INSTANCE.toPo(sim);
        simPo.setModifyBy(SecurityUtils.getUserId().toString());
        return toAjax(simAppService.modifySim(simPo));
    }

    /**
     * 删除SIM卡信息
     *
     * @param simIds SIM卡ID数组
     * @return 结果
     */
    @Log(title = "SIM卡管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("iov:mno:sim:remove")
    @Override
    @DeleteMapping("/{simIds}")
    public AjaxResult remove(@PathVariable Long[] simIds) {
        logger.info("管理后台用户[{}]删除SIM卡信息[{}]", SecurityUtils.getUsername(), simIds);
        return toAjax(simAppService.deleteSimByIds(simIds));
    }

}
