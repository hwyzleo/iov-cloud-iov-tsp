package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.vehicle;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.CreateActivationSessionRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.ReportIviLoginResultRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.ReportPrivacyAuthorizationRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.SkipActivationRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ActivationAgreementsVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ActivationResultVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.CreateActivationSessionVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.LoginTicketVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.PrivacyAuthorizationItemsVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ReportAckVo;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler.ActivationVoAssembler;
import net.hwyz.iov.cloud.iov.tsp.service.adapter.web.support.TrustedCallerResolver;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.ActivationLoginTicketAppService;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.ConsentAppService;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.UserVehicleBindingAppService;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.UserVehicleBindingErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.PrivacyAuthorizationCatalog;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspUserVehicleProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 车端绑车激活入口（IOV-TSP-DSN-CR-017 D017-01/D017-05）。
 * <p>
 * 车辆身份只取可信车端上下文（X-Vin + 车机客户端类型）；不返回通用账号令牌或登录材料。
 * <p>
 * IOV-TSP-DSN-CR-020：新增只读授权项目录端点（D020-02，Accept-Language + Cache-Control）；
 * 会话创建支持专用 {@code Idempotency-Key} 请求头（D020-05，<b>不读取</b>
 * {@code X-Idempotency-Key}，避免被框架内存幂等过滤器遮蔽）。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicle/activation/v1")
public class VehicleActivationController {

    /**
     * 目录响应缓存头（R-20：私有缓存 5 分钟，目录变更靠 catalogVersion 识别，不引入 ETag/Last-Modified）
     */
    private static final String CATALOG_CACHE_CONTROL = "private, max-age=300";

    private final UserVehicleBindingAppService userVehicleBindingAppService;
    private final ConsentAppService consentAppService;
    private final ActivationLoginTicketAppService activationLoginTicketAppService;
    private final PrivacyAuthorizationCatalog privacyAuthorizationCatalog;
    private final TspUserVehicleProperties properties;
    private final TrustedCallerResolver trustedCallerResolver;

    /**
     * 车端只读获取批准隐私授权项目录（IOV-TSP-DSN-CR-020 D020-02）。
     * <p>
     * 无副作用（不写库、不写审计、不调外部服务）；响应字段集固定
     * {@code catalogVersion + items[{authorizationItem, displayName, required}]}（不含 currentVersion）。
     *
     * @param acceptLanguage Accept-Language（可选；缺省或不支持的语言回落 zh-CN）
     * @param request        HTTP 请求（可信车端上下文）
     * @param response       HTTP 响应（写缓存头）
     * @return 目录
     */
    @GetMapping("/privacy-authorization/items")
    public ApiResponse<PrivacyAuthorizationItemsVo> privacyAuthorizationItems(
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage,
            HttpServletRequest request, HttpServletResponse response) {
        String vin = trustedCallerResolver.requireVehicleVin(request);
        if (!properties.getPrivacyAuthorization().isEnabled()) {
            // 回滚开关：目录不可用对车端等价于「不上报、不兜底」（R-5），不阻断进入娱乐系统
            log.warn("隐私授权项目录端点已关闭（enabled=false）: vin={}", maskVin(vin));
            throw new BusinessException(UserVehicleBindingErrorCode.DEPENDENCY_UNAVAILABLE);
        }
        response.setHeader("Cache-Control", CATALOG_CACHE_CONTROL);
        PrivacyAuthorizationItemsVo vo = ActivationVoAssembler.toPrivacyAuthorizationItemsVo(
                privacyAuthorizationCatalog, acceptLanguage);
        log.info("车端[{}]获取隐私授权项目录: catalogVersion={}, itemCount={}", maskVin(vin),
                vo == null ? null : vo.getCatalogVersion(), vo == null || vo.getItems() == null
                        ? 0 : vo.getItems().size());
        return ApiResponse.ok(vo);
    }

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
     * <p>
     * IOV-TSP-DSN-CR-020 D020-05：可选 {@code Idempotency-Key}（1..128 字符，字符集
     * {@code [A-Za-z0-9_.:-]}）；存在但非法 → 401112；缺失 → 每次新建会话（行为不变）。
     *
     * @param request       会话申请
     * @param idempotencyKey 幂等键（请求头 Idempotency-Key，可空）
     * @return 会话与二维码凭据
     */
    @PostMapping("/session")
    public ApiResponse<CreateActivationSessionVo> createSession(@RequestBody @Valid CreateActivationSessionRequest request,
                                                                @RequestHeader(value = "Idempotency-Key", required = false)
                                                                String idempotencyKey,
                                                                HttpServletRequest httpRequest) {
        String vin = trustedCallerResolver.requireVehicleVin(httpRequest);
        log.info("车端[{}]申请激活二维码会话, idempotent={}", maskVin(vin), idempotencyKey != null);
        return ApiResponse.ok(userVehicleBindingAppService.createSession(
                trustedCallerResolver.trustedVehicleRef(), vin, request.getAgreements(), idempotencyKey));
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
     * 申请一次性登录票据（绑定成功且车机登录未完成时，IOV-TSP-DSN-CR-019 D019-02）
     *
     * @param sessionId 会话ID
     * @return 票据原值、到期时刻、会话与关系标识
     */
    @PostMapping("/session/{sessionId}/login-ticket")
    public ApiResponse<LoginTicketVo> issueLoginTicket(@PathVariable String sessionId,
                                                       HttpServletRequest httpRequest) {
        String vin = trustedCallerResolver.requireVehicleVin(httpRequest);
        log.info("车端[{}]申请登录票据, sessionId={}", maskVin(vin), sessionId);
        return ApiResponse.ok(activationLoginTicketAppService.issue(vin, sessionId));
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
     * <p>
     * 终审修复 B-1：{@code catalogVersion} 为<b>条件必填</b>（仅在
     * {@code privacy-authorization.enabled && enforce-catalog} 时必填；兼容模式允许缺失，
     * 回退旧行为并在日志/审计标记 {@code COMPAT_MODE}），校验在 Service 层完成，Controller 不做额外判断。
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
