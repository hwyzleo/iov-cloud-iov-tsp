package net.hwyz.iov.cloud.iov.tsp.service.common.exception;


import net.hwyz.iov.cloud.framework.common.exception.BaseException;

/**
 * 通讯运营商服务基础异常
 *
 * @author hwyz_leo
 */
public class MnoBaseException extends BaseException {

    private static final int ERROR_CODE = 204000;

    public MnoBaseException(String message) {
        super(ERROR_CODE, message);
    }

    public MnoBaseException(int errorCode) {
        super(errorCode);
    }

    public MnoBaseException(int errorCode, String message) {
        super(errorCode, message);
    }

}
