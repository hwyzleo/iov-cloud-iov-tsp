package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptTboxExportVo;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptTboxSummaryResult;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * TSP-DSN-CR-014：MPT Excel 导出展示时区转换测试。
 * <p>
 * 存储/传输为 UTC 墙钟（DB 语义）；导出面向中国运营人员，需转换为展示时区墙钟（默认 Asia/Shanghai）。
 * JSON 接口仍输出 epoch 数字（绝对时刻），不在本类覆盖。
 *
 * @author hwyz_leo
 * @since 2026-10-31
 */
class MptTboxVoAssemblerTest {

    private static final LocalDateTime UTC_WALL = LocalDateTime.of(2026, 10, 10, 8, 0, 0);
    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");

    private final MptTboxVoAssembler assembler = MptTboxVoAssembler.INSTANCE;

    @Test
    void toExportVo_ShouldConvertUtcWallToDisplayZone() {
        MptTboxSummaryResult result = MptTboxSummaryResult.builder()
                .sn("SN-1")
                .firstLoginTime(UTC_WALL)
                .lastLoginTime(UTC_WALL.plusDays(8))
                .activateTime(UTC_WALL.plusDays(30))
                .createdAt(UTC_WALL)
                .updatedAt(UTC_WALL.plusDays(1))
                .build();

        MptTboxExportVo vo = assembler.toExportVo(result, SHANGHAI);

        // 2026-10-10 08:00:00 UTC 墙钟 → 2026-10-10 16:00:00 上海墙钟（+8h，同一绝对时刻）
        assertEquals(LocalDateTime.of(2026, 10, 10, 16, 0, 0), vo.getFirstLoginTime());
        assertEquals(LocalDateTime.of(2026, 10, 10, 16, 0, 0), vo.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 10, 18, 16, 0, 0), vo.getLastLoginTime());
        assertEquals(LocalDateTime.of(2026, 11, 9, 16, 0, 0), vo.getActivateTime());
        // 绝对时刻保持一致（转换不改变时间点）
        assertEquals(UTC_WALL.toInstant(ZoneOffset.UTC), vo.getFirstLoginTime().toInstant(SHANGHAI.getRules().getOffset(UTC_WALL)));
    }

    @Test
    void toExportVo_NullTime_ShouldPassThrough() {
        MptTboxSummaryResult result = MptTboxSummaryResult.builder().sn("SN-1").build();
        MptTboxExportVo vo = assembler.toExportVo(result, SHANGHAI);
        assertNull(vo.getFirstLoginTime());
        assertNull(vo.getCreatedAt());
    }

    @Test
    void toExportVoList_ShouldConvertEachItem() {
        List<MptTboxExportVo> vos = assembler.toExportVoList(List.of(
                MptTboxSummaryResult.builder().sn("A").firstLoginTime(UTC_WALL).build(),
                MptTboxSummaryResult.builder().sn("B").firstLoginTime(UTC_WALL.plusHours(2)).build()),
                SHANGHAI);
        assertEquals(2, vos.size());
        assertEquals(LocalDateTime.of(2026, 10, 10, 16, 0, 0), vos.get(0).getFirstLoginTime());
        assertEquals(LocalDateTime.of(2026, 10, 10, 18, 0, 0), vos.get(1).getFirstLoginTime());
    }
}
