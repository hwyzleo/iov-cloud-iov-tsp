package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.security.util.SecurityUtils;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.util.PageUtil;
import net.hwyz.iov.cloud.iov.tsp.api.vo.SimMpt;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.SimMptAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.SimAppService;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.SimPo;
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
public class MptSimController extends BaseController {

    private final SimAppService simAppService;

    /**
     * 分页查询SIM卡信息
     *
     * @param sim SIM卡信息
     * @return SIM卡信息列表
     */
    @RequiresPermissions("iov:mno:sim:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<SimMpt>> list(SimMpt sim) {
        log.info("管理后台用户[{}]分页查询SIM卡信息", SecurityUtils.getUsername());
        startPage();
        List<SimPo> simPoList = simAppService.search(sim.getIccid(), getBeginTime(sim),
                getEndTime(sim));
        return ApiResponse.ok(getPageResult(PageUtil.convert(simPoList, SimMptAssembler.INSTANCE::fromPo)));
    }

    /**
     * 导出SIM卡信息
     *
     * @param response 响应
     * @param sim      SIM卡信息
     */
    @Log(title = "SIM卡管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("iov:mno:sim:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SimMpt sim) {
        log.info("管理后台用户[{}]导出车辆品牌信息", SecurityUtils.getUsername());
    }

    /**
     * 根据SIM卡ID获取SIM卡信息
     *
     * @param simId SIM卡ID
     * @return SIM卡信息
     */
    @RequiresPermissions("iov:mno:sim:query")
    @GetMapping(value = "/{simId}")
    public ApiResponse<SimMpt> getInfo(@PathVariable Long simId) {
        log.info("管理后台用户[{}]根据SIM卡ID[{}]获取SIM卡信息", SecurityUtils.getUsername(), simId);
        SimPo simPo = simAppService.getSimById(simId);
        return ApiResponse.ok(SimMptAssembler.INSTANCE.fromPo(simPo));
    }

    /**
     * 新增SIM卡信息
     *
     * @param sim SIM卡信息
     * @return 结果
     */
    @Log(title = "SIM卡管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("iov:mno:sim:add")
    @PostMapping
    public ApiResponse<Integer> add(@Validated @RequestBody SimMpt sim) {
        log.info("管理后台用户[{}]新增SIM卡信息[{}]", SecurityUtils.getUsername(), sim.getIccid());
        if (!simAppService.checkIccidUnique(sim.getId(), sim.getIccid())) {
            return ApiResponse.fail("新增SIM卡'" + sim.getIccid() + "'失败，ICCID已存在");
        }
        SimPo simPo = SimMptAssembler.INSTANCE.toPo(sim);
        simPo.setCreateBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(simAppService.createSim(simPo));
    }

    /**
     * 修改保存SIM卡信息
     *
     * @param sim SIM卡信息
     * @return 结果
     */
    @Log(title = "SIM卡管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("iov:mno:sim:edit")
    @PutMapping
    public ApiResponse<Integer> edit(@Validated @RequestBody SimMpt sim) {
        log.info("管理后台用户[{}]修改保存SIM卡信息[{}]", SecurityUtils.getUsername(), sim.getIccid());
        if (!simAppService.checkIccidUnique(sim.getId(), sim.getIccid())) {
            return ApiResponse.fail("修改保存SIM卡'" + sim.getIccid() + "'失败，ICCID已存在");
        }
        SimPo simPo = SimMptAssembler.INSTANCE.toPo(sim);
        simPo.setModifyBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(simAppService.modifySim(simPo));
    }

    /**
     * 删除SIM卡信息
     *
     * @param simIds SIM卡ID数组
     * @return 结果
     */
    @Log(title = "SIM卡管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("iov:mno:sim:remove")
    @DeleteMapping("/{simIds}")
    public ApiResponse<Integer> remove(@PathVariable Long[] simIds) {
        log.info("管理后台用户[{}]删除SIM卡信息[{}]", SecurityUtils.getUsername(), simIds);
        return ApiResponse.ok(simAppService.deleteSimByIds(simIds));
    }

}
