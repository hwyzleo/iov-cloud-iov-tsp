package net.hwyz.iov.cloud.iov.tsp.service.domain.model.entity;

import cn.hutool.core.util.IdUtil;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;
import net.hwyz.iov.cloud.framework.common.domain.BaseDo;
import net.hwyz.iov.cloud.framework.common.domain.DomainObj;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.RemoteControlType;
import net.hwyz.iov.cloud.iov.tsp.service.common.enums.MsgFlow;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 远控领域对象
 *
 * @author hwyz_leo
 */
@Slf4j
@Getter
@SuperBuilder
public class RemoteControl extends BaseDo<String> implements DomainObj<RemoteControl> {

    /**
     * 车架号
     */
    private String vin;
    /**
     * 指令ID
     */
    private String cmdId;
    /**
     * 远控类型
     */
    private RemoteControlType type;
    /**
     * 指令参数
     */
    private Map<String, Object> params;
    /**
     * 消息流向
     */
    private MsgFlow msgFlow;
    /**
     * 消息重试次数
     */
    private Integer msgRetry;
    /**
     * 消息时间
     */
    private Date msgTime;
    /**
     * 第一次下发时间
     */
    private Date firstDownTime;

    /**
     * 初始化
     */
    public void init() {
        stateInit();
        msgRetry = 0;
    }

    /**
     * 处理指令
     *
     * @param remoteControlNew 新远控领域对象
     */
    public void handle(RemoteControl remoteControlNew) {
        log.info("处理车辆[{}]远控指令[{}]", vin, type);
        switch (remoteControlNew.getMsgFlow()) {
            case DOWN -> handleDown(remoteControlNew);
            case UP -> handleUp(remoteControlNew);
            default -> log.warn("车辆[{}]远控指令[{}]消息流向异常", vin, type);
        }
        msgFlow = remoteControlNew.getMsgFlow();
        msgTime = remoteControlNew.getMsgTime();
        stateChange();
    }

    /**
     * 处理下行指令
     *
     * @param remoteControlNew 新远控领域对象
     */
    private void handleDown(RemoteControl remoteControlNew) {
        if (firstDownTime == null) {
            // 第一次下行
            reset(remoteControlNew.getMsgTime());
        } else {
            if (msgFlow == MsgFlow.DOWN && remoteControlNew.getMsgTime().getTime() - firstDownTime.getTime() <= type.getTimeout() * 1000) {
                // 没有上行消息且在超时时间范围内则作为重试下行指令
                if (msgRetry > type.getRetryLimit()) {
                    log.warn("车辆[{}]远控指令[{}]超过重试次数[{}]", vin, type, type.getRetryLimit());
                }
                msgRetry++;
            } else {
                // 其他情况下重置指令
                reset(remoteControlNew.getMsgTime());
            }
        }
        convertParams(remoteControlNew.getType(), remoteControlNew.getParams());
    }

    /**
     * 处理上行指令
     *
     * @param remoteControlNew 新远控领域对象
     */
    private void handleUp(RemoteControl remoteControlNew) {
        if (!cmdId.equals(remoteControlNew.getCmdId())) {
            log.warn("车辆[{}]远控指令[{}]响应指令ID不一致[{}:{}]", vin, type, cmdId, remoteControlNew.getCmdId());
        }
        if (remoteControlNew.getMsgTime().getTime() - firstDownTime.getTime() > type.getTimeout() * 1000) {
            log.warn("车辆[{}]远控指令[{}][{}]超时", vin, type, cmdId);
        }
    }

    /**
     * 重置指令
     */
    private void reset(Date msgTime) {
        firstDownTime = msgTime;
        cmdId = IdUtil.simpleUUID();
        msgRetry = 0;
    }

    /**
     * 转换参数
     *
     * @param type         远控类型
     * @param originParams 原始远控参数
     */
    private void convertParams(RemoteControlType type, Map<String, Object> originParams) {
        Map<String, Object> newParams = new HashMap<>();
        newParams.put("type", type);
        newParams.put("cmdId", cmdId);
        if (originParams != null) {
            newParams.putAll(originParams);
        }
        params = newParams;
    }
}
