package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxAnomalyType;
import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceStatus;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementView;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query.model.AnomalyTag;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * TBOX 运营异常标签派生器
 * <p>
 * TSP-DSN-CR-009：查询时派生，不改变 DeviceStatus。
 * 优先级仅影响展示排序：BINDING_CONFLICT &gt; FROZEN_OR_RETIRED &gt; ACTIVATION_TIMEOUT &gt; NEVER_ONLINE &gt; LONG_OFFLINE &gt; UNBOUND。
 *
 * @author hwyz_leo
 */
@Service
public class TboxAnomalyEvaluator {

    /**
     * 派生异常标签（按优先级排序）
     *
     * @param view                   TBOX 聚合视图
     * @param now                    服务端当前时间
     * @param activationTimeout      激活超时阈值
     * @param longOfflineThreshold   长期未登录阈值
     * @return 异常标签列表
     */
    public List<AnomalyTag> evaluate(TboxManagementView view, LocalDateTime now,
                                     Duration activationTimeout, Duration longOfflineThreshold) {
        List<AnomalyTag> tags = new ArrayList<>();
        if (view == null) {
            return tags;
        }
        int bindCount = view.getBindCount() == null ? 0 : view.getBindCount();
        DeviceStatus status = resolveStatus(view.getDeviceStatus());

        if (bindCount > 1) {
            tags.add(new AnomalyTag(TboxAnomalyType.BINDING_CONFLICT, now, null, null));
        }
        if (status == DeviceStatus.BLOCKED || status == DeviceStatus.RETIRED) {
            tags.add(new AnomalyTag(TboxAnomalyType.FROZEN_OR_RETIRED, now, null, null));
        }
        if (status == DeviceStatus.ACTIVATING
                && view.getFirstLoginTime() != null
                && activationTimeout != null
                && view.getFirstLoginTime().isBefore(now.minus(activationTimeout))) {
            tags.add(new AnomalyTag(TboxAnomalyType.ACTIVATION_TIMEOUT, now,
                    view.getFirstLoginTime(), secondsBetween(view.getFirstLoginTime(), now)));
        }
        if (view.getFirstLoginTime() == null) {
            tags.add(new AnomalyTag(TboxAnomalyType.NEVER_ONLINE, now, view.getCreateTime(),
                    view.getCreateTime() != null ? secondsBetween(view.getCreateTime(), now) : null));
        }
        if (view.getLastLoginTime() != null
                && longOfflineThreshold != null
                && view.getLastLoginTime().isBefore(now.minus(longOfflineThreshold))) {
            tags.add(new AnomalyTag(TboxAnomalyType.LONG_OFFLINE, now,
                    view.getLastLoginTime(), secondsBetween(view.getLastLoginTime(), now)));
        }
        if (bindCount == 0) {
            tags.add(new AnomalyTag(TboxAnomalyType.UNBOUND, now, null, null));
        }

        tags.sort(Comparator.comparingInt(tag -> tag.type().getPriority()));
        return tags;
    }

    private DeviceStatus resolveStatus(Integer code) {
        try {
            return DeviceStatus.fromCode(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private Long secondsBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return null;
        }
        return ChronoUnit.SECONDS.between(from, to);
    }
}
