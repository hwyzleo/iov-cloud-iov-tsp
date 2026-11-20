package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.c;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.ConfirmVehicleBindingRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.ScanActivationSessionRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.BindingOperationVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ConfirmVehicleBindingVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ScanActivationSessionVo;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.support.TrustedCallerResolver;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.UserVehicleBindingAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端（App）绑车激活入口（IOV-TSP-DSN-CR-017 D017-05）。
 * <p>
 * 账号取可信认证上下文，不接受请求体自报账号；二维码/操作查询按调用方账号限定，
 * 日志不记录明文二维码或账号令牌。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/c/activation/v1")
public class CActivationController {

    private final UserVehicleBindingAppService userVehicleBindingAppService;
    private final TrustedCallerResolver trustedCallerResolver;

    /**
     * 扫码认领会话并获取最小待确认车辆信息
     *
     * @param request 扫码请求
     * @return 最小车辆信息
     */
    @PostMapping("/session/scan")
    public ApiResponse<ScanActivationSessionVo> scan(@RequestBody @Valid ScanActivationSessionRequest request) {
        String accountId = trustedCallerResolver.requireAppAccount();
        log.info("用户[{}]扫码认领激活会话", accountId);
        return ApiResponse.ok(userVehicleBindingAppService.scan(accountId, request.getQrToken()));
    }

    /**
     * 取消会话（不修改已有云端关系）
     *
     * @param sessionId 会话ID
     * @return 是否已取消
     */
    @PostMapping("/session/cancel")
    public ApiResponse<Boolean> cancel(@RequestParam String sessionId) {
        String accountId = trustedCallerResolver.requireAppAccount();
        log.info("用户[{}]取消激活会话[{}]", accountId, sessionId);
        userVehicleBindingAppService.cancel(accountId, sessionId);
        return ApiResponse.ok(Boolean.TRUE);
    }

    /**
     * 确认绑定（同步资格核验 + 幂等原子提交）
     *
     * @param request 确认请求
     * @return 绑定结果
     */
    @PostMapping("/binding/confirm")
    public ApiResponse<ConfirmVehicleBindingVo> confirm(@RequestBody @Valid ConfirmVehicleBindingRequest request) {
        String accountId = trustedCallerResolver.requireAppAccount();
        log.info("用户[{}]确认绑定会话[{}], idempotencyKey={}", accountId, request.getSessionId(),
                request.getIdempotencyKey());
        return ApiResponse.ok(userVehicleBindingAppService.confirm(accountId, request));
    }

    /**
     * 查询绑定操作结果（仅本人操作）
     *
     * @param operationId 操作ID
     * @return 操作结果
     */
    @GetMapping("/binding/operations/{operationId}")
    public ApiResponse<BindingOperationVo> operation(@PathVariable String operationId) {
        String accountId = trustedCallerResolver.requireAppAccount();
        log.info("用户[{}]查询绑定操作[{}]", accountId, operationId);
        return ApiResponse.ok(userVehicleBindingAppService.queryOperation(accountId, operationId));
    }
}
