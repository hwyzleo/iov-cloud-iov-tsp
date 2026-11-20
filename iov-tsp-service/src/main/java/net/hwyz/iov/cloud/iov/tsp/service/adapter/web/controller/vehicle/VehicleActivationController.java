package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.vehicle;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.CreateActivationSessionRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.ReportIviLoginResultRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.ReportPrivacyAuthorizationRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.SkipActivationRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ActivationAgreementsVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ActivationResultVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.CreateActivationSessionVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ReportAckVo;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.ActivationVoAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.support.TrustedCallerResolver;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.ConsentAppService;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.UserVehicleBindingAppService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车端绑车激活入口（IOV-TSP-DSN-CR-017 D017-01/D017-05）。
 * <p>
 * 车辆身份只取可信车端上下文（X-Vin + 车机客户端类型）；不返回通用账号令牌或登录材料。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicle/activation/v1")
public class VehicleActivationController {

    private final UserVehicleBindingAppService userVehicleBindingAppService;
    private final ConsentAppService consentAppService;
    private final TrustedCallerResolver trustedCallerResolver;

    /**
     * 查询激活协议（两项已发布协议及可追溯版本）
     *
     * @param language 语言
     * @return 协议内容
     */
    @GetMapping("/agreements")
    public ApiResponse<ActivationAgreementsVo> agreements(@RequestParam(required = false) String language,
                                                          HttpServletRequest request) {
        String vin = trustedCallerResolver.requireVehicleVin(request);
        log.info("车端[{}]查询激活协议", maskVin(vin));
        return ApiResponse.ok(ActivationVoAssembler.toAgreementsVo(
                userVehicleBindingAppService.queryAgreements(language)));
    }

    /**
     * 申请激活二维码会话（两项协议必须明确同意）
     *
     * @param request 会话申请
     * @return 会话与二维码凭据
     */
    @PostMapping("/session")
    public ApiResponse<CreateActivationSessionVo> createSession(@RequestBody @Valid CreateActivationSessionRequest request,
                                                                HttpServletRequest httpRequest) {
        String vin = trustedCallerResolver.requireVehicleVin(httpRequest);
        log.info("车端[{}]申请激活二维码会话", maskVin(vin));
        return ApiResponse.ok(userVehicleBindingAppService.createSession(
                trustedCallerResolver.trustedVehicleRef(), vin, request.getAgreements()));
    }

    /**
     * 跳过激活：仅记录 VIN 级同意，不建用户关系
     *
     * @param request 跳过请求
     * @return 回执结果
     */
    @PostMapping("/session/skip")
    public ApiResponse<ReportAckVo> skip(@RequestBody @Valid SkipActivationRequest request,
                                         HttpServletRequest httpRequest) {
        String vin = trustedCallerResolver.requireVehicleVin(httpRequest);
        log.info("车端[{}]跳过激活, reportId={}", maskVin(vin), request.getReportId());
        return ApiResponse.ok(consentAppService.skipActivation(vin, request));
    }

    /**
     * 查询本车绑定结果（独立恢复窗口）
     *
     * @param sessionId 会话ID
     * @return 绑定结果
     */
    @GetMapping("/session/{sessionId}/result")
    public ApiResponse<ActivationResultVo> result(@PathVariable String sessionId, HttpServletRequest request) {
        String vin = trustedCallerResolver.requireVehicleVin(request);
        log.info("车端[{}]查询会话[{}]绑定结果", maskVin(vin), sessionId);
        return ApiResponse.ok(userVehicleBindingAppService.queryVehicleResult(vin, sessionId));
    }

    /**
     * 回报车机账号登录结果（登录失败不改变云端关系）
     *
     * @param request 登录结果回执
     * @return 回执结果
     */
    @PostMapping("/session/ivi-login-result")
    public ApiResponse<ReportAckVo> reportIviLoginResult(@RequestBody @Valid ReportIviLoginResultRequest request,
                                                         HttpServletRequest httpRequest) {
        String vin = trustedCallerResolver.requireVehicleVin(httpRequest);
        log.info("车端[{}]回报车机登录结果, sessionId={}, reportId={}", maskVin(vin),
                request.getSessionId(), request.getReportId());
        return ApiResponse.ok(consentAppService.reportIviLoginResult(vin, request));
    }

    /**
     * 补报隐私授权（Account + VIN + 授权项）
     *
     * @param request 授权回执
     * @return 回执结果
     */
    @PostMapping("/privacy-authorization")
    public ApiResponse<ReportAckVo> reportPrivacyAuthorization(
            @RequestBody @Valid ReportPrivacyAuthorizationRequest request, HttpServletRequest httpRequest) {
        String vin = trustedCallerResolver.requireVehicleVin(httpRequest);
        String accountId = trustedCallerResolver.requireAccountId();
        log.info("车端[{}]补报隐私授权, account={}, reportId={}", maskVin(vin), accountId, request.getReportId());
        return ApiResponse.ok(consentAppService.reportPrivacyAuthorization(accountId, vin, request));
    }

    private String maskVin(String vin) {
        if (vin == null || vin.length() <= 7) {
            return "***";
        }
        return vin.substring(0, 3) + "***" + vin.substring(vin.length() - 4);
    }
}
