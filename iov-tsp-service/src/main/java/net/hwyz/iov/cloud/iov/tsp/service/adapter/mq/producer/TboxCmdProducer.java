package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.producer;

import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * TBOX指令消息生产者
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TboxCmdProducer {

    private final ReactiveKafkaProducerTemplate<String, String> stringReactiveKafkaProducerTemplate;

    private final String TOPIC_TBOX_VAGW_CMD = "tbox-vagw-cmd";

    /**
     * 发送TBOX指令至车辆接入网关
     *
     * @param vin 车架号
     * @param cmd TBOX指令
     */
    public void send(String vin, Map<String, Object> cmd) {
        String cmdJson = JSONUtil.toJsonStr(cmd);
        log.debug("向车辆[{}]发送TBOX指令[{}]至车辆接入网关", vin, cmdJson);
        stringReactiveKafkaProducerTemplate.send(TOPIC_TBOX_VAGW_CMD, vin, cmdJson)
                .doOnError(throwable -> log.error("向车辆[{}]发送TBOX指令[{}]至车辆接入网关异常[{}]", vin, cmdJson, throwable.getMessage()))
                .subscribe();
    }

}
