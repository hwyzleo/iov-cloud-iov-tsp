package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.bean.ApiResponse;
import net.hwyz.iov.cloud.framework.common.constant.CustomHeaders;
import net.hwyz.iov.cloud.framework.common.util.ServletUtil;
import net.hwyz.iov.cloud.iov.tsp.api.vo.request.ConsumeLoginTicketRequest;
import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ConsumeLoginTicketResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.ActivationLoginTicketAppService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 一次性登录票据服务域入口（IOV-TSP-DSN-CR-019 D019-03）。
 * <p>
 * 唯一消费者为 SEC-CIAM：票据消费为原子单次条件更新，成功返回绑定四要素与关系标识，
 * 不返回任何令牌。按确认口径本端点不做调用方白名单校验，信任边界由内网服务域承担；
 * 调用方服务名仅作为审计记录的消费方。
 *
 * @author hwyz_leo
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/service/activation/v1")
public class ServiceActivationTicketController {

    private final ActivationLoginTicketAppService activationLoginTicketAppService;

    /**
     * 消费一次性登录票据（仅一次成功，无业务副作用）
     *
     * @param request     消费请求（仅票据原值）
     * @param httpRequest HTTP 请求（读取调用方服务名用于审计）
     * @return 票据绑定的事实
     */
    @PostMapping("/login-ticket/consume")
    public ApiResponse<ConsumeLoginTicketResult> consume(@RequestBody @Valid ConsumeLoginTicketRequest request,
                                                         HttpServletRequest httpRequest) {
        String callerService = ServletUtil.getHeader(httpRequest, CustomHeaders.CALLER_SERVICE_NAME);
        log.info("消费登录票据, caller={}", callerService);
        return ApiResponse.ok(activationLoginTicketAppService.consume(request.getTicket(), callerService));
    }
}
