package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxTimelineMilestone;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementView;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxRegistrationEventRecord;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query.model.TimelineMilestone;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.service.DeviceRegistrationDomainService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * TBOX 生命周期时间线构建器
 * <p>
 * TSP-DSN-CR-009：由设备字段与消费幂等记录（Inbox）组合可证实里程碑；
 * 缺少来源事实时省略，不根据当前状态伪造历史。BLOCKED/RETIRED 无权威发生时间来源，因此不生成。
 *
 * @author hwyz_leo
 */
@Service
public class TboxTimelineBuilder {

    /**
     * 构建设备可证实时间线（按时间升序）
     *
     * @param view   设备聚合视图
     * @param events 消费幂等记录；Inbox 不可用时可传 null
     * @return 里程碑列表
     */
    public List<TimelineMilestone> build(TboxManagementView view, List<TboxRegistrationEventRecord> events) {
        List<TimelineMilestone> items = new ArrayList<>();
        if (view == null) {
            return items;
        }
        List<TboxRegistrationEventRecord> safeEvents = events == null ? List.of() : events;
        List<TboxRegistrationEventRecord> onlineEvents = safeEvents.stream()
                .filter(event -> DeviceRegistrationDomainService.EVENT_TYPE_DEVICE_ONLINE.equals(event.eventType()))
                .filter(event -> event.occurredAt() != null)
                .sorted(Comparator.comparing(TboxRegistrationEventRecord::occurredAt))
                .toList();
        List<TboxRegistrationEventRecord> v2cEvents = safeEvents.stream()
                .filter(event -> DeviceRegistrationDomainService.EVENT_TYPE_V2C_KEY_PROVISIONED.equals(event.eventType()))
                .filter(event -> event.occurredAt() != null)
                .sorted(Comparator.comparing(TboxRegistrationEventRecord::occurredAt))
                .toList();

        if (view.getCreateTime() != null) {
            items.add(new TimelineMilestone(TboxTimelineMilestone.IMPORTED, view.getCreateTime(), null, "设备导入"));
        }

        TboxRegistrationEventRecord firstOnline = onlineEvents.isEmpty() ? null : onlineEvents.get(0);
        TboxRegistrationEventRecord lastOnline = onlineEvents.isEmpty() ? null : onlineEvents.get(onlineEvents.size() - 1);

        if (view.getFirstLoginTime() != null) {
            String sourceRef = firstOnline != null ? firstOnline.eventId() : null;
            items.add(new TimelineMilestone(TboxTimelineMilestone.FIRST_LOGIN, view.getFirstLoginTime(),
                    sourceRef, "首次 mTLS + 准入成功建链"));
            items.add(new TimelineMilestone(TboxTimelineMilestone.ACTIVATING, view.getFirstLoginTime(),
                    sourceRef, "进入 ACTIVATING"));
        }

        for (TboxRegistrationEventRecord v2cEvent : v2cEvents) {
            items.add(new TimelineMilestone(TboxTimelineMilestone.V2C_PROVISIONED, toLocalDateTime(v2cEvent.occurredAt()),
                    v2cEvent.eventId(), "VMD V2C 聚合完成"));
        }

        if (view.getActivateTime() != null) {
            String sourceRef = v2cEvents.isEmpty() ? null : v2cEvents.get(v2cEvents.size() - 1).eventId();
            items.add(new TimelineMilestone(TboxTimelineMilestone.ACTIVE, view.getActivateTime(),
                    sourceRef, "设备已激活"));
        }

        if (view.getLastLoginTime() != null) {
            String sourceRef = lastOnline != null ? lastOnline.eventId() : null;
            items.add(new TimelineMilestone(TboxTimelineMilestone.LAST_LOGIN, view.getLastLoginTime(),
                    sourceRef, "最近一次成功建链"));
        }

        items.sort(Comparator.comparing(TimelineMilestone::occurredAt));
        return items;
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant != null ? LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }
}
