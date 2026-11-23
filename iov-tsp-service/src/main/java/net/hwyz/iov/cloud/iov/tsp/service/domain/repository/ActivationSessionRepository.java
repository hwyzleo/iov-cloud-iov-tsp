package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.IviLoginResultStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationSession;

import java.time.Instant;
import java.util.Optional;

/**
 * 一次性激活会话仓储。
 *
 * @author hwyz_leo
 */
public interface ActivationSessionRepository {

    /**
     * 新增会话
     *
     * @param session 会话
     */
    void insert(ActivationSession session);

    /**
     * 按会话业务ID查询
     *
     * @param sessionId 会话业务ID
     * @return 会话
     */
    Optional<ActivationSession> findBySessionId(String sessionId);

    /**
     * 按二维码摘要查询
     *
     * @param qrTokenHash 二维码摘要
     * @return 会话
     */
    Optional<ActivationSession> findByQrTokenHash(String qrTokenHash);

    /**
     * 锁定读取（事务内使用，外部 RPC 不得在此之后执行）
     *
     * @param sessionId 会话业务ID
     * @return 会话
     */
    Optional<ActivationSession> lockBySessionId(String sessionId);

    /**
     * 条件认领账号（ISSUED → SCANNED）
     *
     * @param sessionId      会话业务ID
     * @param accountId      账号
     * @param now            当前时刻
     * @param expectedVersion 期望版本
     * @return 更新成功返回 true
     */
    boolean claim(String sessionId, String accountId, Instant now, int expectedVersion);

    /**
     * 条件取消
     *
     * @param sessionId       会话业务ID
     * @param accountId       账号（可为空，车端取消）
     * @param now             当前时刻
     * @param expectedVersion 期望版本
     * @return 更新成功返回 true
     */
    boolean cancel(String sessionId, String accountId, Instant now, int expectedVersion);

    /**
     * 条件标记绑定完成（SCANNED → BOUND）
     *
     * @param sessionId        会话业务ID
     * @param operationId      绑定操作ID
     * @param now              当前时刻
     * @param resultExpiresAt  结果恢复截止时刻
     * @param expectedVersion  期望版本
     * @return 更新成功返回 true
     */
    boolean markBound(String sessionId, String operationId, Instant now, Instant resultExpiresAt, int expectedVersion);

    /**
     * 条件标记跳过（ISSUED/SCANNED → SKIPPED）
     *
     * @param sessionId       会话业务ID
     * @param now             当前时刻
     * @param expectedVersion 期望版本
     * @return 更新成功返回 true
     */
    boolean markSkipped(String sessionId, Instant now, int expectedVersion);

    /**
     * 记录车机登录结果（仅同车同会话，幂等回执）
     *
     * @param sessionId  会话业务ID
     * @param status     登录结果
     * @param reportId   回执ID
     * @param now        接收时刻
     * @return 更新成功返回 true
     */
    boolean reportIviLoginResult(String sessionId, IviLoginResultStatus status, String reportId, Instant now);

    /**
     * 同会话重签二维码（IOV-TSP-DSN-CR-020 D020-05 幂等回放）：
     * 只存新码不可逆摘要，旧码立即失效；会话必须仍为 ISSUED。
     *
     * @param sessionId       会话业务ID
     * @param qrTokenHash     新二维码摘要
     * @param now             当前时刻
     * @param expectedVersion 期望版本
     * @return 更新成功返回 true
     */
    boolean resignQrToken(String sessionId, String qrTokenHash, Instant now, int expectedVersion);
}
