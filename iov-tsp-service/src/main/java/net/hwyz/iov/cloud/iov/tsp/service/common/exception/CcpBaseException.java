package net.hwyz.iov.cloud.iov.tsp.service.common.exception;


import net.hwyz.iov.cloud.framework.common.exception.BaseException;

/**
 * 中央计算平台服务基础异常
 *
 * @author hwyz_leo
 */
public class CcpBaseException extends BaseException {

    private static final int ERROR_CODE = 206000;

    public CcpBaseException(String message) {
        super(ERROR_CODE, message);
    }

    public CcpBaseException(int errorCode) {
        super(errorCode);
    }

    public CcpBaseException(int errorCode, String message) {
        super(errorCode, message);
    }

}
