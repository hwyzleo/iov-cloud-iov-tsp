package net.hwyz.iov.cloud.iov.tsp.service.common.exception;


import net.hwyz.iov.cloud.framework.common.exception.BaseException;

/**
 * 车联终端服务基础异常
 *
 * @author hwyz_leo
 */
public class TboxBaseException extends BaseException {

    private static final int ERROR_CODE = 205000;

    public TboxBaseException(String message) {
        super(ERROR_CODE, message);
    }

    public TboxBaseException(int errorCode) {
        super(errorCode);
    }

    public TboxBaseException(int errorCode, String message) {
        super(errorCode, message);
    }

}
