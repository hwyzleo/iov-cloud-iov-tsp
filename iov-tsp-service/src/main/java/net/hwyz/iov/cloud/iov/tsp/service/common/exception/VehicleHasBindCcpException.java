package net.hwyz.iov.cloud.iov.tsp.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 车辆已绑定中央计算平台异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class VehicleHasBindCcpException extends CcpBaseException {

    private static final int ERROR_CODE = 206001;

    public VehicleHasBindCcpException(String vin, String oldSn, String newSn) {
        super(ERROR_CODE);
        log.warn("车辆[{}]已绑定中央计算平台[{}]无法绑定新中央计算平台[{}]", vin, oldSn, newSn);
    }

}
