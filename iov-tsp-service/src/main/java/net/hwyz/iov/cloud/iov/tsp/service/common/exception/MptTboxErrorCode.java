package net.hwyz.iov.cloud.iov.tsp.service.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.hwyz.iov.cloud.framework.common.exception.ErrorCode;

/**
 * TBOX 后台运营（MPT）错误码
 * <p>
 * TSP-DSN-CR-009：只读查询与导出相关稳定错误码。
 *
 * @author hwyz_leo
 */
@Getter
@AllArgsConstructor
public enum MptTboxErrorCode implements ErrorCode {

    /**
     * TBOX 不存在
     */
    TBOX_NOT_FOUND("205002", "TBOX不存在"),

    /**
     * 查询参数非法（如排序字段不在白名单）
     */
    INVALID_QUERY_PARAM("205003", "查询参数非法"),

    /**
     * 导出超过平台允许的最大行数
     */
    EXPORT_LIMIT_EXCEEDED("205004", "导出数据量超过平台限制");

    private final String code;
    private final String message;
}
