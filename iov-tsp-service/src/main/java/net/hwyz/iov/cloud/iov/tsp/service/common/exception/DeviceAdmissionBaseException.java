package net.hwyz.iov.cloud.iov.tsp.service.common.exception;

import net.hwyz.iov.cloud.framework.common.exception.BaseException;

/**
 * 设备接入鉴权基础异常
 *
 * @author hwyz_leo
 */
public class DeviceAdmissionBaseException extends BaseException {

    public DeviceAdmissionBaseException(String message) {
        super(207000, message);
    }

    public DeviceAdmissionBaseException(int code, String message) {
        super(code, message);
    }
}
