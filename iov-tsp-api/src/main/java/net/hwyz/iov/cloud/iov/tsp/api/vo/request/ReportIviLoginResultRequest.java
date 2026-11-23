package net.hwyz.iov.cloud.iov.tsp.api.vo.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.IviLoginFailureReason;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.IviLoginResultStatus;

/**
 * 可信车端回报车机账号登录结果。
 * <p>
 * IOV-TSP-DSN-CR-017 D017-05/D017-06：必须关联已提交绑定与会话 VIN；
 * 失败只影响车机登录，不改变云端关系。
 * <p>
 * IOV-TSP-DSN-CR-020 D020-06：{@code bindingOperationId} 由类级 {@code @NotBlank}
 * 改为<b>条件必填</b>（SUCCEEDED 必填且必须与会话一致；FAILED 可省略，缺号仍要记录失败事实），
 * 由应用服务统一校验以复用 401112 语义；新增可选 {@code failureReason}（仅 FAILED 有意义）。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportIviLoginResultRequest {

    /**
     * 会话ID
     */
    @NotBlank(message = "会话ID不能为空")
    private String sessionId;

    /**
     * 绑定操作ID（条件必填：loginStatus=SUCCEEDED 必填且必须与会话一致，否则 401112；
     * loginStatus=FAILED 可省略，缺号仍发送/接收 FAILED，按会话 VIN 归属校验）
     */
    private String bindingOperationId;

    /**
     * 回执ID（幂等键）
     */
    @NotBlank(message = "回执ID不能为空")
    private String reportId;

    /**
     * 登录结果
     */
    @NotNull(message = "登录结果不能为空")
    private IviLoginResultStatus loginStatus;

    /**
     * 登录失败原因（可选，仅 FAILED 有意义；取值集由 IviLoginFailureReason 冻结）
     */
    private IviLoginFailureReason failureReason;

    /**
     * 用户/车机动作时间（ISO-8601、带 Z/offset）
     */
    private String occurredAt;

    /**
     * 登录成功时实际展示并明确同意的两项协议修订证据（登录失败时可为空）
     */
    @Valid
    private java.util.List<AgreementAcceptanceRequest> agreements;
}
