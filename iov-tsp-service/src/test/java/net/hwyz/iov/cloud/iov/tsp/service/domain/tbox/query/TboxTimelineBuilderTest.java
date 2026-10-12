package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxTimelineMilestone;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementView;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxRegistrationEventRecord;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query.model.TimelineMilestone;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TBOX 生命周期时间线构建测试（TSP-DSN-CR-009）
 */
class TboxTimelineBuilderTest {

    private static final LocalDateTime IMPORT = LocalDateTime.of(2026, 9, 1, 8, 0, 0);
    private static final LocalDateTime FIRST_LOGIN = LocalDateTime.of(2026, 9, 2, 8, 0, 0);
    private static final LocalDateTime ACTIVATE = LocalDateTime.of(2026, 9, 2, 8, 5, 0);
    private static final LocalDateTime LAST_LOGIN = LocalDateTime.of(2026, 9, 10, 8, 0, 0);

    private final TboxTimelineBuilder builder = new TboxTimelineBuilder();

    private TboxRegistrationEventRecord event(String eventId, String eventType, LocalDateTime occurredAt) {
        return new TboxRegistrationEventRecord(eventId, null, "SN-1", eventType,
                occurredAt.atZone(ZoneId.systemDefault()).toInstant(), occurredAt, "SUCCEEDED", null);
    }

    @Test
    void build_ShouldCombineFieldAndInboxMilestonesInAscendingOrder() {
        TboxManagementView view = TboxManagementView.builder()
                .sn("SN-1")
                .createTime(IMPORT)
                .firstLoginTime(FIRST_LOGIN)
                .activateTime(ACTIVATE)
                .lastLoginTime(LAST_LOGIN)
                .build();
        List<TboxRegistrationEventRecord> events = List.of(
                event("EVT-ONLINE-1", "DEVICE_ONLINE", FIRST_LOGIN),
                event("EVT-ONLINE-2", "DEVICE_ONLINE", LAST_LOGIN),
                event("EVT-V2C", "TBOX_V2C_KEY_PROVISIONED", ACTIVATE));

        List<TimelineMilestone> milestones = builder.build(view, events);
        List<TboxTimelineMilestone> types = milestones.stream().map(TimelineMilestone::milestone).toList();

        assertEquals(List.of(
                TboxTimelineMilestone.IMPORTED,
                TboxTimelineMilestone.FIRST_LOGIN,
                TboxTimelineMilestone.ACTIVATING,
                TboxTimelineMilestone.V2C_PROVISIONED,
                TboxTimelineMilestone.ACTIVE,
                TboxTimelineMilestone.LAST_LOGIN), types);

        // FIRST_LOGIN / ACTIVATING 共享 sourceRef
        TimelineMilestone firstLogin = milestones.get(1);
        TimelineMilestone activating = milestones.get(2);
        assertEquals("EVT-ONLINE-1", firstLogin.sourceRef());
        assertEquals("EVT-ONLINE-1", activating.sourceRef());

        // V2C_PROVISIONED / ACTIVE 共享 sourceRef
        assertEquals("EVT-V2C", milestones.get(3).sourceRef());
        assertEquals("EVT-V2C", milestones.get(4).sourceRef());

        // LAST_LOGIN 使用最近一次 DEVICE_ONLINE
        assertEquals("EVT-ONLINE-2", milestones.get(5).sourceRef());
    }

    @Test
    void build_WithoutInbox_ShouldStillReturnFieldMilestones() {
        TboxManagementView view = TboxManagementView.builder()
                .sn("SN-1")
                .createTime(IMPORT)
                .firstLoginTime(FIRST_LOGIN)
                .activateTime(ACTIVATE)
                .lastLoginTime(LAST_LOGIN)
                .build();

        List<TimelineMilestone> milestones = builder.build(view, null);
        List<TboxTimelineMilestone> types = milestones.stream().map(TimelineMilestone::milestone).toList();

        assertEquals(List.of(
                TboxTimelineMilestone.IMPORTED,
                TboxTimelineMilestone.FIRST_LOGIN,
                TboxTimelineMilestone.ACTIVATING,
                TboxTimelineMilestone.ACTIVE,
                TboxTimelineMilestone.LAST_LOGIN), types);
        assertNull(milestones.get(1).sourceRef());
    }

    @Test
    void build_ShouldNotFabricateBlockedOrRetired() {
        TboxManagementView view = TboxManagementView.builder()
                .sn("SN-1")
                .deviceStatus(4)
                .createTime(IMPORT)
                .firstLoginTime(FIRST_LOGIN)
                .build();

        List<TboxTimelineMilestone> types = builder.build(view, List.of()).stream()
                .map(TimelineMilestone::milestone).toList();

        assertTrue(types.stream().noneMatch(t -> t == TboxTimelineMilestone.BLOCKED
                || t == TboxTimelineMilestone.RETIRED));
    }

    @Test
    void build_EmptyView_ShouldReturnEmpty() {
        assertTrue(builder.build(TboxManagementView.builder().sn("SN-1").build(), List.of()).isEmpty());
    }
}
