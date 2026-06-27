package net.hwyz.iov.cloud.iov.tsp.service.common.exception;

/**
 * HSM-VIN绑定关系未找到异常
 *
 * @author hwyz_leo
 */
public class HSMVinBindingNotFoundException extends DeviceAdmissionBaseException {

    public HSMVinBindingNotFoundException(String message) {
        super(207002, message);
    }
}
