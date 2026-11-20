package net.hwyz.iov.cloud.iov.tsp.api.vo.enums;

/**
 * 一次性激活会话状态（IOV-TSP-DSN-CR-017 D017-02）。
 * <p>
 * 迁移路径：ISSUED → SCANNED → BOUND；ISSUED/SCANNED → CANCELLED/EXPIRED/SKIPPED。
 * BOUND 是扫码绑定终态；车机登录结果另行记录，登录失败不把关系改回未绑定。
 *
 * @author hwyz_leo
 */
public enum ActivationSessionState {

    /**
     * 已签发二维码，等待扫码
     */
    ISSUED,

    /**
     * 已被账号认领，等待确认
     */
    SCANNED,

    /**
     * 已完成绑定（终态）
     */
    BOUND,

    /**
     * 已取消
     */
    CANCELLED,

    /**
     * 已过期（服务端 Clock 判定）
     */
    EXPIRED,

    /**
     * 车端跳过激活（仅记录 VIN 级同意，不建用户关系）
     */
    SKIPPED
}
