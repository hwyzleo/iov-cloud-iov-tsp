package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.TboxAnomalyType;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementView;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query.model.AnomalyTag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TBOX 异常标签派生测试（TSP-DSN-CR-009）
 */
class TboxAnomalyEvaluatorTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 12, 12, 0, 0);
    private static final Duration ACTIVATION_TIMEOUT = Duration.ofMinutes(30);
    private static final Duration LONG_OFFLINE = Duration.ofDays(30);

    private final TboxAnomalyEvaluator evaluator = new TboxAnomalyEvaluator();

    /**
     * IOV-TSP-DSN-CR-016：默认构造「VIAM 投影已同步」视图，保证安全状态标签可派生；
     * 投影缺失场景单独覆盖。
     */
    private TboxManagementView.TboxManagementViewBuilder syncedView() {
        return TboxManagementView.builder().viamSyncStatus("SYNCED");
    }

    private List<TboxAnomalyType> types(List<AnomalyTag> tags) {
        return tags.stream().map(AnomalyTag::type).toList();
    }

    @Test
    void bindingConflict_ShouldBeHighestPriority_AndHaveNoDuration() {
        TboxManagementView view = syncedView()
                .deviceStatus(2).bindCount(2).firstLoginTime(NOW).build();

        List<AnomalyTag> tags = evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE);

        assertEquals(List.of(TboxAnomalyType.BINDING_CONFLICT), types(tags));
        assertNull(tags.get(0).since());
        assertNull(tags.get(0).durationSeconds());
        assertEquals(NOW, tags.get(0).evaluatedAt());
    }

    @Test
    void frozenOrRetired_ShouldDeriveTag() {
        TboxManagementView blocked = syncedView()
                .deviceStatus(4).bindCount(1).firstLoginTime(NOW).build();
        TboxManagementView retired = syncedView()
                .deviceStatus(3).bindCount(1).firstLoginTime(NOW).build();

        assertTrue(types(evaluator.evaluate(blocked, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE))
                .contains(TboxAnomalyType.FROZEN_OR_RETIRED));
        assertTrue(types(evaluator.evaluate(retired, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE))
                .contains(TboxAnomalyType.FROZEN_OR_RETIRED));
    }

    @Test
    void activationTimeout_ShouldUseFirstLoginAsSince() {
        LocalDateTime firstLogin = NOW.minusMinutes(31);
        TboxManagementView view = syncedView()
                .deviceStatus(5).bindCount(1).firstLoginTime(firstLogin).build();

        List<AnomalyTag> tags = evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE);

        assertEquals(List.of(TboxAnomalyType.ACTIVATION_TIMEOUT), types(tags));
        assertEquals(firstLogin, tags.get(0).since());
        assertEquals(31 * 60L, tags.get(0).durationSeconds());
    }

    @Test
    void activationTimeout_ShouldNotTriggerAtBoundary() {
        TboxManagementView view = syncedView()
                .deviceStatus(5).bindCount(1).firstLoginTime(NOW.minus(ACTIVATION_TIMEOUT)).build();

        assertFalse(types(evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE))
                .contains(TboxAnomalyType.ACTIVATION_TIMEOUT));
    }

    @Test
    void neverOnline_ShouldUseCreateTimeAsSince() {
        LocalDateTime created = NOW.minusDays(2);
        TboxManagementView view = syncedView()
                .deviceStatus(1).bindCount(1).createTime(created).build();

        List<AnomalyTag> tags = evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE);

        AnomalyTag tag = tags.stream().filter(t -> t.type() == TboxAnomalyType.NEVER_ONLINE).findFirst().orElseThrow();
        assertEquals(created, tag.since());
        assertEquals(2 * 24 * 60 * 60L, tag.durationSeconds());
    }

    @Test
    void neverOnline_WithoutCreateTime_DurationIsNull() {
        TboxManagementView view = syncedView().deviceStatus(1).bindCount(1).build();

        AnomalyTag tag = evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE).stream()
                .filter(t -> t.type() == TboxAnomalyType.NEVER_ONLINE).findFirst().orElseThrow();

        assertNull(tag.since());
        assertNull(tag.durationSeconds());
    }

    @Test
    void longOffline_ShouldUseLastLoginAsSince() {
        LocalDateTime lastLogin = NOW.minusDays(31);
        TboxManagementView view = syncedView()
                .deviceStatus(2).bindCount(1).firstLoginTime(lastLogin).lastLoginTime(lastLogin).build();

        List<AnomalyTag> tags = evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE);

        assertEquals(List.of(TboxAnomalyType.LONG_OFFLINE), types(tags));
        assertEquals(lastLogin, tags.get(0).since());
        assertEquals(31 * 24 * 60 * 60L, tags.get(0).durationSeconds());
    }

    @Test
    void unbound_ShouldDeriveTag() {
        TboxManagementView view = syncedView()
                .deviceStatus(2).bindCount(0).firstLoginTime(NOW).build();

        List<AnomalyTag> tags = evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE);

        assertEquals(List.of(TboxAnomalyType.UNBOUND), types(tags));
        assertNull(tags.get(0).durationSeconds());
    }

    @Test
    void multipleTags_ShouldBeOrderedByPriority() {
        TboxManagementView view = syncedView()
                .deviceStatus(4).bindCount(2).build();

        List<TboxAnomalyType> types = types(evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE));

        assertEquals(List.of(
                TboxAnomalyType.BINDING_CONFLICT,
                TboxAnomalyType.FROZEN_OR_RETIRED,
                TboxAnomalyType.NEVER_ONLINE), types);
    }

    @Test
    void nullView_ShouldReturnEmpty() {
        assertTrue(evaluator.evaluate(null, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE).isEmpty());
    }

    /**
     * IOV-TSP-DSN-CR-016：VIAM 投影缺失（viamSyncStatus=null）时安全字段不可信，
     * 不派生 NEVER_ONLINE / FROZEN_OR_RETIRED / ACTIVATION_TIMEOUT / LONG_OFFLINE；
     * 仅保留与投影无关的 BINDING_CONFLICT/UNBOUND。
     */
    @Test
    void projectionMissing_ShouldNotDeriveSecurityTags() {
        TboxManagementView view = TboxManagementView.builder()
                .bindCount(1).build(); // 无 viamSyncStatus = 从未同步；first/lastLoginTime 均未知

        List<AnomalyTag> tags = evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE);

        assertFalse(types(tags).contains(TboxAnomalyType.NEVER_ONLINE), "投影缺失不得误判 NEVER_ONLINE");
        assertFalse(types(tags).contains(TboxAnomalyType.FROZEN_OR_RETIRED));
        assertFalse(types(tags).contains(TboxAnomalyType.ACTIVATION_TIMEOUT));
        assertFalse(types(tags).contains(TboxAnomalyType.LONG_OFFLINE));
    }

    @Test
    void projectionMissing_WithBlockedCode_ShouldNotDeriveFrozen() {
        // 即使 device_status 残留旧值，投影缺失也不得按旧状态伪装最新事实
        TboxManagementView view = TboxManagementView.builder()
                .deviceStatus(4).bindCount(1).firstLoginTime(NOW).build();

        List<AnomalyTag> tags = evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE);

        assertFalse(types(tags).contains(TboxAnomalyType.FROZEN_OR_RETIRED));
    }
}
