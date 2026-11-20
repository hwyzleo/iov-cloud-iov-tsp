package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.ActivationSessionState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.IviLoginResultStatus;

import java.time.Instant;

/**
 * 一次性激活会话（IOV-TSP-DSN-CR-017 D017-02）。
 * <p>
 * 数据库保存权威状态；状态迁移一律使用状态/版本条件更新，到期判定使用服务端 Clock，
 * 不依赖后台清理任务。二维码只保存不可逆摘要。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationSession {

    private Long id;

    /**
     * 会话业务ID
     */
    private String sessionId;

    /**
     * 二维码凭据不可逆摘要
     */
    private String qrTokenHash;

    /**
     * 可信车端引用（不含 PII）
     */
    private String trustedVehicleRef;

    /**
     * 可信车端上下文 VIN
     */
    private String vin;

    /**
     * 展示的两项协议修订证据（agreementType:revisionId）
     */
    private String agreementRevisions;

    /**
     * 到期绝对时刻
     */
    private Instant expiresAt;

    /**
     * 会话状态
     */
    private ActivationSessionState state;

    /**
     * 认领账号
     */
    private String claimedAccountId;

    /**
     * 认领时刻
     */
    private Instant claimedAt;

    /**
     * 绑定操作ID
     */
    private String bindingOperationId;

    /**
     * 跳过时刻
     */
    private Instant skippedAt;

    /**
     * 取消时刻
     */
    private Instant cancelledAt;

    /**
     * 绑定成功时刻
     */
    private Instant boundAt;

    /**
     * 车端结果恢复窗口截止时刻
     */
    private Instant resultExpiresAt;

    /**
     * 车机登录结果（独立于绑定状态）
     */
    private IviLoginResultStatus iviLoginState;

    /**
     * 车机登录结果回执ID
     */
    private String iviLoginReportId;

    /**
     * 车机登录结果接收时刻
     */
    private Instant iviLoginReportedAt;

    /**
     * 乐观版本
     */
    private Integer rowVersion;

    /**
     * 是否已到期（服务端 Clock 判定）
     *
     * @param now 当前绝对时刻
     * @return 已到期返回 true
     */
    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    /**
     * 是否可被指定账号认领：未认领或同一账号，且状态/到期仍有效
     *
     * @param accountId 可信账号
     * @param now       当前绝对时刻
     * @return 可认领返回 true
     */
    public boolean claimableBy(String accountId, Instant now) {
        if (state != ActivationSessionState.ISSUED && state != ActivationSessionState.SCANNED) {
            return false;
        }
        if (isExpired(now)) {
            return false;
        }
        return claimedAccountId == null || claimedAccountId.equals(accountId);
    }

    /**
     * 是否可确认绑定：必须由认领账号在有效期内确认，且未取消/跳过
     *
     * @param accountId 可信账号
     * @param now       当前绝对时刻
     * @return 可确认返回 true
     */
    public boolean confirmableBy(String accountId, Instant now) {
        if (state != ActivationSessionState.SCANNED) {
            return false;
        }
        if (isExpired(now)) {
            return false;
        }
        return accountId != null && accountId.equals(claimedAccountId);
    }
}
