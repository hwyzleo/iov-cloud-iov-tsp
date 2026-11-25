package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.mpt;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.audit.annotation.Log;
import net.hwyz.iov.cloud.framework.audit.enums.BusinessType;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.bean.PageResult;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.framework.security.annotation.RequiresPermissions;
import net.hwyz.iov.cloud.framework.web.context.SecurityContextHolder;
import net.hwyz.iov.cloud.framework.web.controller.BaseController;
import net.hwyz.iov.cloud.framework.web.timezone.DisplayTimeFacade;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptUnbindOperationVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptUserVehicleHistoryVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptUserVehicleRelationVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptUserVehicleUnbindVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.MptUserVehicleHistoryRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.MptUserVehicleUnbindRequest;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.MptUserVehicleVoAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUnbindOperationResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUserVehicleHistoryResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.MptUserVehicleQueryAppService;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.UserVehicleUnbindAppService;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.ZoneId;
import java.util.List;

/**
 * MPT 车辆管理「用户绑定/激活」后台运营接口（IOV-TSP-DSN-CR-021）。
 * <p>
 * 只读查询权限 {@code tsp:user-vehicle:query}；写操作权限 {@code tsp:user-vehicle:unbind}。
 * 页面归属车辆管理，事实仍按 TSP/VMD/CIAM 各自所有权读取：
 * 用户关系与车机登录来自 TSP，本地关系释放为 TSP 强事务，首次激活来自 VMD 只读契约，
 * 凭证撤销由 CIAM 幂等命令与 TSP 台账补偿。
 * <p>
 * 写响应返回绝对 UTC 时刻；详情/历史/操作查询在 VO 边界按 CR-015 输出生效展示时区墙钟。
 * 日志与指标不使用完整 accountId/VIN，不记录票据或 token。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/mpt/userVehicle/v1")
public class MptUserVehicleController extends BaseController {

    private static final String PERMISSION_QUERY = "tsp:user-vehicle:query";
    private static final String PERMISSION_UNBIND = "tsp:user-vehicle:unbind";

    private final MptUserVehicleQueryAppService mptUserVehicleQueryAppService;
    private final UserVehicleUnbindAppService userVehicleUnbindAppService;
    private final DisplayTimeFacade displayTimeFacade;

    /**
     * 查询指定 VIN 的用户绑定、车机登录与首次激活数据区
     *
     * @param vin        车辆 VIN
     * @param httpRequest HTTP 请求（展示时区）
     * @return 详情数据区
     */
    @RequiresPermissions(PERMISSION_QUERY)
    @GetMapping("/relation")
    public ApiResponse<MptUserVehicleRelationVo> relation(
            @RequestParam(required = false) String vin, HttpServletRequest httpRequest) {
        // 缺失/非法 VIN 统一在应用层按 401112 拒绝，避免框架参数异常与业务码分叉
        ZoneId zone = displayTimeFacade.resolveEffectiveZone(httpRequest);
        log.info("管理后台用户[{}]查询车辆[{}]用户关系详情", currentOperator(), maskVin(vin));
        return ApiResponse.ok(MptUserVehicleVoAssembler.INSTANCE.toVo(
                mptUserVehicleQueryAppService.getRelation(vin), zone));
    }

    /**
     * 分页查询用户车辆关系历史（数据库侧过滤与物理分页）
     *
     * @param request     查询请求
     * @param httpRequest HTTP 请求（展示时区）
     * @return 分页历史
     */
    @RequiresPermissions(PERMISSION_QUERY)
    @GetMapping("/relation/history")
    public ApiResponse<PageResult<MptUserVehicleHistoryVo>> history(MptUserVehicleHistoryRequest request,
                                                                   HttpServletRequest httpRequest) {
        ZoneId zone = displayTimeFacade.resolveEffectiveZone(httpRequest);
        PageResult<MptUserVehicleHistoryResult> page = mptUserVehicleQueryAppService.searchHistory(request);
        List<MptUserVehicleHistoryVo> items = page.getItems().stream()
                .map(result -> MptUserVehicleVoAssembler.INSTANCE.toHistoryVo(result, zone))
                .toList();
        log.info("管理后台用户[{}]查询用户关系历史, total={}", currentOperator(), page.getTotal());
        return ApiResponse.ok(PageResult.<MptUserVehicleHistoryVo>builder()
                .items(items)
                .total(page.getTotal())
                .page(page.getPage())
                .size(page.getSize())
                .build());
    }

    /**
     * 人工解绑指定关系（二次确认 + CAS + 幂等 + 审计）
     *
     * @param relationId 关系ID
     * @param request    解绑请求
     * @return 关系与凭证撤销两个独立状态（绝对 UTC）
     */
    @RequiresPermissions(PERMISSION_UNBIND)
    @Log(title = "用户车辆关系", businessType = BusinessType.UPDATE)
    @PostMapping("/relation/{relationId}/unbind")
    public ApiResponse<MptUserVehicleUnbindVo> unbind(@PathVariable String relationId,
                                                      @RequestBody @Valid MptUserVehicleUnbindRequest request) {
        String operatorId = requireOperator();
        log.info("管理后台用户[{}]人工解绑关系[{}], rowVersion={}", operatorId, relationId, request.getRowVersion());
        return ApiResponse.ok(MptUserVehicleVoAssembler.INSTANCE.toUnbindVo(
                userVehicleUnbindAppService.unbind(operatorId, relationId, request)));
    }

    /**
     * 查询解绑操作权威结果（查询不到不得被解释为失败）
     *
     * @param operationId 解绑操作ID
     * @param httpRequest HTTP 请求（展示时区）
     * @return 操作结果
     */
    @RequiresPermissions(PERMISSION_QUERY)
    @GetMapping("/unbind/operations/{operationId}")
    public ApiResponse<MptUnbindOperationVo> operation(@PathVariable String operationId,
                                                       HttpServletRequest httpRequest) {
        ZoneId zone = displayTimeFacade.resolveEffectiveZone(httpRequest);
        log.info("管理后台用户[{}]查询解绑操作[{}]", currentOperator(), operationId);
        return ApiResponse.ok(MptUserVehicleVoAssembler.INSTANCE.toOperationVo(
                mptUserVehicleQueryAppService.queryOperation(operationId), zone));
    }

    /**
     * 人工重试凭证撤销（沿用原 operationId，不再次迁移关系）
     *
     * @param operationId 解绑操作ID
     * @return 最新权威结果（绝对 UTC）
     */
    @RequiresPermissions(PERMISSION_UNBIND)
    @Log(title = "用户车辆关系", businessType = BusinessType.UPDATE)
    @PostMapping("/unbind/operations/{operationId}/retry-revocation")
    public ApiResponse<MptUserVehicleUnbindVo> retryRevocation(@PathVariable String operationId) {
        String operatorId = requireOperator();
        log.info("管理后台用户[{}]人工重试凭证撤销[{}]", operatorId, operationId);
        return ApiResponse.ok(MptUserVehicleVoAssembler.INSTANCE.toUnbindVo(
                userVehicleUnbindAppService.retryRevocation(operatorId, operationId)));
    }

    private String requireOperator() {
        String operator = currentOperator();
        if (operator == null || operator.isBlank()) {
            // 写操作必须有可信操作人，否则审计与 released_by 不可追溯
            throw new BusinessException(UserVehicleBindingErrorCode.INVALID_REQUEST);
        }
        return operator;
    }

    private String currentOperator() {
        try {
            return SecurityContextHolder.getUserName();
        } catch (Exception e) {
            return null;
        }
    }

    private String maskVin(String vin) {
        if (vin == null || vin.length() <= 7) {
            return "***";
        }
        return vin.substring(0, 3) + "***" + vin.substring(vin.length() - 4);
    }
}
