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
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.vo.SimMpt;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.SimMptAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.cmd.SimCmd;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.query.SimQuery;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.SimResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.SimAppService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/mpt/sim")
public class MptSimController extends BaseController {

    private final SimAppService simAppService;

    @RequiresPermissions("iov:mno:sim:list")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<SimMpt>> list(SimMpt sim) {
        log.info("管理后台用户[{}]分页查询SIM卡信息", SecurityUtils.getUsername());
        startPage();
        SimQuery query = SimMptAssembler.INSTANCE.toQuery(sim);
        List<SimResult> resultList = simAppService.search(query);
        return ApiResponse.ok(getPageResult(PageUtil.convert(resultList, SimMptAssembler.INSTANCE::toVo)));
    }

    @Log(title = "SIM卡管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("iov:mno:sim:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SimMpt sim) {
        log.info("管理后台用户[{}]导出车辆品牌信息", SecurityUtils.getUsername());
    }

    @RequiresPermissions("iov:mno:sim:query")
    @GetMapping(value = "/{simId}")
    public ApiResponse<SimMpt> getInfo(@PathVariable Long simId) {
        log.info("管理后台用户[{}]根据SIM卡ID[{}]获取SIM卡信息", SecurityUtils.getUsername(), simId);
        SimResult result = simAppService.getById(simId);
        return ApiResponse.ok(SimMptAssembler.INSTANCE.toVo(result));
    }

    @Log(title = "SIM卡管理", businessType = BusinessType.INSERT)
    @RequiresPermissions("iov:mno:sim:add")
    @PostMapping
    public ApiResponse<Integer> add(@Validated @RequestBody SimMpt sim) {
        log.info("管理后台用户[{}]新增SIM卡信息[{}]", SecurityUtils.getUsername(), sim.getIccid());
        if (!simAppService.checkIccidUnique(sim.getId(), sim.getIccid())) {
            return ApiResponse.fail("新增SIM卡'" + sim.getIccid() + "'失败，ICCID已存在");
        }
        SimCmd cmd = SimMptAssembler.INSTANCE.toCmd(sim);
        cmd.setCreateBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(simAppService.create(cmd));
    }

    @Log(title = "SIM卡管理", businessType = BusinessType.UPDATE)
    @RequiresPermissions("iov:mno:sim:edit")
    @PutMapping
    public ApiResponse<Integer> edit(@Validated @RequestBody SimMpt sim) {
        log.info("管理后台用户[{}]修改保存SIM卡信息[{}]", SecurityUtils.getUsername(), sim.getIccid());
        if (!simAppService.checkIccidUnique(sim.getId(), sim.getIccid())) {
            return ApiResponse.fail("修改保存SIM卡'" + sim.getIccid() + "'失败，ICCID已存在");
        }
        SimCmd cmd = SimMptAssembler.INSTANCE.toCmd(sim);
        cmd.setModifyBy(SecurityUtils.getUserId().toString());
        return ApiResponse.ok(simAppService.update(cmd));
    }

    @Log(title = "SIM卡管理", businessType = BusinessType.DELETE)
    @RequiresPermissions("iov:mno:sim:remove")
    @DeleteMapping("/{simIds}")
    public ApiResponse<Integer> remove(@PathVariable Long[] simIds) {
        log.info("管理后台用户[{}]删除SIM卡信息[{}]", SecurityUtils.getUsername(), simIds);
        return ApiResponse.ok(simAppService.deleteByIds(simIds));
    }

}