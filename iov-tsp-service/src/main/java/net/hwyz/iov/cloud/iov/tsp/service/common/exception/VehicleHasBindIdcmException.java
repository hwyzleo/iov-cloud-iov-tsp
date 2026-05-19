package net.hwyz.iov.cloud.iov.tsp.service.common.exception;

import lombok.extern.slf4j.Slf4j;

/**
 * 车辆已绑定信息娱乐模块异常
 *
 * @author hwyz_leo
 */
@Slf4j
public class VehicleHasBindIdcmException extends IdcmBaseException {

    private static final int ERROR_CODE = 207001;

    public VehicleHasBindIdcmException(String vin, String oldSn, String newSn) {
        super(ERROR_CODE);
        log.warn("车辆[{}]已绑定娱乐模块异常[{}]无法绑定新娱乐模块异常[{}]", vin, oldSn, newSn);
    }

}
