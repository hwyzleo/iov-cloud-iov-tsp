package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

/**
 * 解绑后 CIAM 凭证撤销状态（IOV-TSP-DSN-CR-021 D021-07）。
 * <p>
 * 关系释放与凭证撤销是两个独立事实：RELEASED + PENDING/FAILED 表示关系已释放、
 * 撤销未完成，不是关系回滚，也不能被解释为全部成功。
 *
 * @author hwyz_leo
 */
public enum CredentialRevocationStatus {

    /**
     * 撤销请求已登记，等待或正在重试
     */
    PENDING,

    /**
     * CIAM 已确认目标撤销态收敛
     */
    SUCCEEDED,

    /**
     * 撤销失败（技术失败或响应非法），保留台账等待自动/人工重试
     */
    FAILED
}
