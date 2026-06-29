package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import net.hwyz.iov.cloud.iov.tsp.api.vo.event.CcsSimStatusChangedEvent;

/**
 * SIM状态事件翻译器（防腐层）
 * 将CCS枚举映射到TSP运营态，不外泄CCS枚举到领域层
 *
 * @author hwyz_leo
 */
@Slf4j
@Component
public class SimStatusEventTranslator {

    /**
     * 翻译CCS SIM状态事件到TSP运营态
     *
     * @param event CCS SIM状态变更事件
     * @return 翻译后的事件（包含TSP运营态）
     */
    public CcsSimStatusChangedEvent translate(CcsSimStatusChangedEvent event) {
        if (event == null) {
            return null;
        }

        log.debug("翻译CCS SIM状态事件: vin={}, iccid={}, simStatus={}, realnameStatus={}",
                event.getVin(), event.getIccid(), event.getSimStatus(), event.getRealnameStatus());

        // CCS枚举映射到TSP运营态
        // 取值映射以CCS契约为准，集中在防腐层translator
        Integer tspSimStatus = translateSimStatus(event.getSimStatus());
        Integer tspRealnameStatus = translateRealnameStatus(event.getRealnameStatus());

        return CcsSimStatusChangedEvent.builder()
                .vin(event.getVin())
                .iccid(event.getIccid())
                .simStatus(tspSimStatus)
                .realnameStatus(tspRealnameStatus)
                .version(event.getVersion())
                .build();
    }

    /**
     * 翻译SIM卡状态
     * CCS sim_status -> TSP SIM生命周期态（测试/库存/激活等）
     */
    private Integer translateSimStatus(Integer ccsSimStatus) {
        if (ccsSimStatus == null) {
            return null;
        }
        // TODO: 根据CCS契约实现具体映射逻辑
        // 示例映射：
        // CCS 1 -> TSP 1 (测试)
        // CCS 2 -> TSP 2 (库存)
        // CCS 3 -> TSP 3 (激活)
        // CCS 4 -> TSP 4 (停用)
        return ccsSimStatus;
    }

    /**
     * 翻译实名状态
     * CCS realname_status -> TSP 实名态
     */
    private Integer translateRealnameStatus(Integer ccsRealnameStatus) {
        if (ccsRealnameStatus == null) {
            return null;
        }
        // TODO: 根据CCS契约实现具体映射逻辑
        // 示例映射：
        // CCS 0 -> TSP 0 (未实名)
        // CCS 1 -> TSP 1 (已实名)
        return ccsRealnameStatus;
    }
}
