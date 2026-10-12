package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.common.util.ExcelUtil;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptTboxDetailVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptTboxExportVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptTboxSummaryVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptTboxTimelineVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.MptTboxQueryRequest;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.MptTboxVoAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptTboxSummaryResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.MptTboxQueryAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * MPT TBOX 后台运营只读接口
 * <p>
 * TSP-DSN-CR-009：仅提供列表/详情/时间线/导出，不提供人工激活、时间修改、绑定、密钥、冻结/退役等写操作。
 * 权限：列表/详情/时间线 tsp:tbox:query；导出 tsp:tbox:export。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/tbox/v1")
public class MptTboxController extends BaseController {

    private final MptTboxQueryAppService mptTboxQueryAppService;

    /**
     * 分页查询 TBOX 列表
     *
     * @param request 查询请求
     * @return TBOX 列表
     */
    @RequiresPermissions("tsp:tbox:query")
    @GetMapping(value = "/list")
    public ApiResponse<PageResult<MptTboxSummaryVo>> list(MptTboxQueryRequest request) {
        log.info("管理后台用户[{}]分页查询TBOX列表", SecurityContextHolder.getUserName());
        PageResult<MptTboxSummaryResult> page = mptTboxQueryAppService.search(request);
        List<MptTboxSummaryVo> items = page.getItems().stream().map(MptTboxVoAssembler.INSTANCE::toVo).toList();
        PageResult<MptTboxSummaryVo> voPage = PageResult.<MptTboxSummaryVo>builder()
                .items(items)
                .total(page.getTotal())
                .page(page.getPage())
                .size(page.getSize())
                .build();
        return ApiResponse.ok(voPage);
    }

    /**
     * 查询 TBOX 运营详情
     *
     * @param sn 设备序列号
     * @return TBOX 详情
     */
    @RequiresPermissions("tsp:tbox:query")
    @GetMapping(value = "/{sn}")
    public ApiResponse<MptTboxDetailVo> detail(@PathVariable String sn) {
        log.info("管理后台用户[{}]查询TBOX详情[{}]", SecurityContextHolder.getUserName(), sn);
        return ApiResponse.ok(MptTboxVoAssembler.INSTANCE.toVo(mptTboxQueryAppService.getDetail(sn)));
    }

    /**
     * 查询 TBOX 生命周期时间线
     *
     * @param sn 设备序列号
     * @return TBOX 时间线
     */
    @RequiresPermissions("tsp:tbox:query")
    @GetMapping(value = "/{sn}/timeline")
    public ApiResponse<MptTboxTimelineVo> timeline(@PathVariable String sn) {
        log.info("管理后台用户[{}]查询TBOX时间线[{}]", SecurityContextHolder.getUserName(), sn);
        return ApiResponse.ok(MptTboxVoAssembler.INSTANCE.toVo(mptTboxQueryAppService.getTimeline(sn)));
    }

    /**
     * 导出 TBOX 列表（沿用查询权限与脱敏结果）
     *
     * @param response 响应
     * @param request  查询请求
     */
    @Log(title = "TBOX管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("tsp:tbox:export")
    @GetMapping(value = "/export")
    public void export(HttpServletResponse response, MptTboxQueryRequest request) {
        log.info("管理后台用户[{}]导出TBOX列表", SecurityContextHolder.getUserName());
        List<MptTboxExportVo> exportList = MptTboxVoAssembler.INSTANCE.toExportVoList(mptTboxQueryAppService.export(request));
        ExcelUtil<MptTboxExportVo> util = new ExcelUtil<>(MptTboxExportVo.class);
        util.exportExcel(response, exportList, "TBOX数据");
    }
}
