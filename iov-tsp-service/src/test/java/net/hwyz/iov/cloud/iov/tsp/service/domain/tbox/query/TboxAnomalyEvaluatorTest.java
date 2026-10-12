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

    private List<TboxAnomalyType> types(List<AnomalyTag> tags) {
        return tags.stream().map(AnomalyTag::type).toList();
    }

    @Test
    void bindingConflict_ShouldBeHighestPriority_AndHaveNoDuration() {
        TboxManagementView view = TboxManagementView.builder()
                .deviceStatus(2).bindCount(2).firstLoginTime(NOW).build();

        List<AnomalyTag> tags = evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE);

        assertEquals(List.of(TboxAnomalyType.BINDING_CONFLICT), types(tags));
        assertNull(tags.get(0).since());
        assertNull(tags.get(0).durationSeconds());
        assertEquals(NOW, tags.get(0).evaluatedAt());
    }

    @Test
    void frozenOrRetired_ShouldDeriveTag() {
        TboxManagementView blocked = TboxManagementView.builder()
                .deviceStatus(4).bindCount(1).firstLoginTime(NOW).build();
        TboxManagementView retired = TboxManagementView.builder()
                .deviceStatus(3).bindCount(1).firstLoginTime(NOW).build();

        assertTrue(types(evaluator.evaluate(blocked, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE))
                .contains(TboxAnomalyType.FROZEN_OR_RETIRED));
        assertTrue(types(evaluator.evaluate(retired, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE))
                .contains(TboxAnomalyType.FROZEN_OR_RETIRED));
    }

    @Test
    void activationTimeout_ShouldUseFirstLoginAsSince() {
        LocalDateTime firstLogin = NOW.minusMinutes(31);
        TboxManagementView view = TboxManagementView.builder()
                .deviceStatus(5).bindCount(1).firstLoginTime(firstLogin).build();

        List<AnomalyTag> tags = evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE);

        assertEquals(List.of(TboxAnomalyType.ACTIVATION_TIMEOUT), types(tags));
        assertEquals(firstLogin, tags.get(0).since());
        assertEquals(31 * 60L, tags.get(0).durationSeconds());
    }

    @Test
    void activationTimeout_ShouldNotTriggerAtBoundary() {
        TboxManagementView view = TboxManagementView.builder()
                .deviceStatus(5).bindCount(1).firstLoginTime(NOW.minus(ACTIVATION_TIMEOUT)).build();

        assertFalse(types(evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE))
                .contains(TboxAnomalyType.ACTIVATION_TIMEOUT));
    }

    @Test
    void neverOnline_ShouldUseCreateTimeAsSince() {
        LocalDateTime created = NOW.minusDays(2);
        TboxManagementView view = TboxManagementView.builder()
                .deviceStatus(1).bindCount(1).createTime(created).build();

        List<AnomalyTag> tags = evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE);

        AnomalyTag tag = tags.stream().filter(t -> t.type() == TboxAnomalyType.NEVER_ONLINE).findFirst().orElseThrow();
        assertEquals(created, tag.since());
        assertEquals(2 * 24 * 60 * 60L, tag.durationSeconds());
    }

    @Test
    void neverOnline_WithoutCreateTime_DurationIsNull() {
        TboxManagementView view = TboxManagementView.builder().deviceStatus(1).bindCount(1).build();

        AnomalyTag tag = evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE).stream()
                .filter(t -> t.type() == TboxAnomalyType.NEVER_ONLINE).findFirst().orElseThrow();

        assertNull(tag.since());
        assertNull(tag.durationSeconds());
    }

    @Test
    void longOffline_ShouldUseLastLoginAsSince() {
        LocalDateTime lastLogin = NOW.minusDays(31);
        TboxManagementView view = TboxManagementView.builder()
                .deviceStatus(2).bindCount(1).firstLoginTime(lastLogin).lastLoginTime(lastLogin).build();

        List<AnomalyTag> tags = evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE);

        assertEquals(List.of(TboxAnomalyType.LONG_OFFLINE), types(tags));
        assertEquals(lastLogin, tags.get(0).since());
        assertEquals(31 * 24 * 60 * 60L, tags.get(0).durationSeconds());
    }

    @Test
    void unbound_ShouldDeriveTag() {
        TboxManagementView view = TboxManagementView.builder()
                .deviceStatus(2).bindCount(0).firstLoginTime(NOW).build();

        List<AnomalyTag> tags = evaluator.evaluate(view, NOW, ACTIVATION_TIMEOUT, LONG_OFFLINE);

        assertEquals(List.of(TboxAnomalyType.UNBOUND), types(tags));
        assertNull(tags.get(0).durationSeconds());
    }

    @Test
    void multipleTags_ShouldBeOrderedByPriority() {
        TboxManagementView view = TboxManagementView.builder()
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
}
