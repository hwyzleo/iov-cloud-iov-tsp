package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.controller.service;

import net.hwyz.iov.cloud.iov.tsp.api.vo.response.ConsumeLoginTicketResult;
import net.hwyz.iov.cloud.iov.tsp.service.application.service.ActivationLoginTicketAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 登录票据服务域入口契约测试（IOV-TSP-DSN-CR-019 TC019-05）。
 * <p>
 * 验证消费端点只接收票据原值、返回绑定四要素与关系标识；调用方服务名仅透传用于审计，
 * 本端点不做调用方白名单校验（按用户确认口径）。
 */
class ServiceActivationTicketControllerTest {

    private ActivationLoginTicketAppService appService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        appService = mock(ActivationLoginTicketAppService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ServiceActivationTicketController(appService)).build();
    }

    @Test
    @DisplayName("消费票据：只传票据原值，返回四要素与关系标识")
    void consumeReturnsBindingFacts() throws Exception {
        Instant expiresAt = Instant.parse("2026-11-22T10:03:00Z");
        when(appService.consume(eq("TICKET-VALUE"), eq("sec-ciam"))).thenReturn(ConsumeLoginTicketResult.builder()
                .accountId("account-1").vin("LSVAA1234567890AB").sessionId("SES-1")
                .bindingOperationId("BOP-1").relationId("REL-1").expiresAt(expiresAt).build());

        mockMvc.perform(post("/api/service/activation/v1/login-ticket/consume")
                        .header("X-Caller-Service", "sec-ciam")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticket\":\"TICKET-VALUE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").value("account-1"))
                .andExpect(jsonPath("$.data.vin").value("LSVAA1234567890AB"))
                .andExpect(jsonPath("$.data.sessionId").value("SES-1"))
                .andExpect(jsonPath("$.data.bindingOperationId").value("BOP-1"))
                .andExpect(jsonPath("$.data.relationId").value("REL-1"));

        verify(appService).consume("TICKET-VALUE", "sec-ciam");
    }

    @Test
    @DisplayName("消费票据：缺失调用方头时传空值（应用层回退配置消费方，不做准入校验）")
    void consumeWithoutCallerHeaderPassesEmpty() throws Exception {
        when(appService.consume(eq("TICKET-VALUE"), eq(""))).thenReturn(ConsumeLoginTicketResult.builder()
                .accountId("account-1").vin("LSVAA1234567890AB").sessionId("SES-1")
                .bindingOperationId("BOP-1").build());

        mockMvc.perform(post("/api/service/activation/v1/login-ticket/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticket\":\"TICKET-VALUE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").value("account-1"));

        verify(appService).consume("TICKET-VALUE", "");
    }

    @Test
    @DisplayName("消费票据：空票据参数校验失败，不进入应用服务")
    void consumeRejectsBlankTicket() throws Exception {
        mockMvc.perform(post("/api/service/activation/v1/login-ticket/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ticket\":\" \"}"))
                .andExpect(status().isBadRequest());

        verify(appService, org.mockito.Mockito.never()).consume(anyString(), anyString());
    }
}
