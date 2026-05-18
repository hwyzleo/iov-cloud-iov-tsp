package net.hwyz.iov.cloud.iov.tsp.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 车辆已绑定车联终端异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class VehicleHasBindTboxException extends TboxBaseException {

    private static final int ERROR_CODE = 205001;

    public VehicleHasBindTboxException(String vin, String oldSn, String newSn) {
        super(ERROR_CODE);
        log.warn("车辆[{}]已绑定车联终端[{}]无法绑定新车联终端[{}]", vin, oldSn, newSn);
    }

}
