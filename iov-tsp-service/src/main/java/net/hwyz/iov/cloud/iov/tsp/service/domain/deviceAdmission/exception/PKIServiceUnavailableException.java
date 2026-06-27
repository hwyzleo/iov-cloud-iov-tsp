package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.exception;

/**
 * PKI服务不可用异常
 *
 * @author hwyz_leo
 */
public class PKIServiceUnavailableException extends DeviceAdmissionBaseException {

    public PKIServiceUnavailableException(String message) {
        super(207001, message);
    }
}
