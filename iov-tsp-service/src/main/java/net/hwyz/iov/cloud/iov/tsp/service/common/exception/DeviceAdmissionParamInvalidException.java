package net.hwyz.iov.cloud.iov.tsp.service.common.exception;

/**
 * 设备接入准入参数非法异常（TSP-DSN-CR-011 §4.2）
 * <p>
 * 参数非法（缺字段、Base64/DER 非法、解码后超过 8 KiB 等）走接口级 401001。
 *
 * @author hwyz_leo
 */
public class DeviceAdmissionParamInvalidException extends DeviceAdmissionBaseException {

    public DeviceAdmissionParamInvalidException(String message) {
        super(401001, message);
    }
}
