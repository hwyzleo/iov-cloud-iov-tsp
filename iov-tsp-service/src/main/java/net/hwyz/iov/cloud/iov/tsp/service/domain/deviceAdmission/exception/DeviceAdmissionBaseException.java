package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.exception;

import net.hwyz.iov.cloud.iov.tsp.service.common.exception.MnoBaseException;

/**
 * 设备接入鉴权基础异常
 *
 * @author hwyz_leo
 */
public class DeviceAdmissionBaseException extends MnoBaseException {

    private static final int ERROR_CODE = 207000;

    public DeviceAdmissionBaseException(String message) {
        super(ERROR_CODE, message);
    }

    public DeviceAdmissionBaseException(int code, String message) {
        super(code, message);
    }
}
