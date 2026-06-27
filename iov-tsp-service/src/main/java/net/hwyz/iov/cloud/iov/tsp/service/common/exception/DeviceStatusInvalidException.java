package net.hwyz.iov.cloud.iov.tsp.service.common.exception;

/**
 * 设备状态无效异常
 *
 * @author hwyz_leo
 */
public class DeviceStatusInvalidException extends DeviceAdmissionBaseException {

    public DeviceStatusInvalidException(String message) {
        super(207003, message);
    }
}
