package net.hwyz.iov.cloud.iov.tsp.service.adapter.web.assembler;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.CredentialRevocationStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.FirstActivationState;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.IviLoginResultStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.MptUserVehicleDetailStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.UserVehicleBindingStatus;
import net.hwyz.iov.cloud.iov.tsp.api.vo.mpt.MptUserVehicleRelationVo;
import net.hwyz.iov.cloud.iov.tsp.service.application.dto.result.MptUserVehicleRelationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MPT 用户车辆关系 VO 投影测试（IOV-TSP-DSN-CR-021 TC021-16）。
 * <p>
 * 验证展示时区仅在 VO 边界转换一次、账号脱敏、绝对事实不被回写。
 */
class MptUserVehicleVoAssemblerTest {

    private static final ZoneId SHANGHAI = ZoneId.of("Asia/Shanghai");
    private static final Instant BOUND_AT = Instant.parse("2026-11-25T02:00:00Z");

    @Test
    @DisplayName("账号脱敏：前 3 后 2，不输出完整账号")
    void masksAccountId() {
        MptUserVehicleRelationResult result = MptUserVehicleRelationResult.builder()
                .vin("LSVAA1234567890AB")
                .bindingStatus(UserVehicleBindingStatus.BOUND)
                .detailStatus(MptUserVehicleDetailStatus.COMPLETE)
                .relation(MptUserVehicleRelationResult.RelationResult.builder()
                        .relationId("REL-1").accountId("account-01").role("PRIMARY_USER").state("ACTIVE")
                        .boundAt(BOUND_AT).rowVersion(3).build())
                .iviLogin(MptUserVehicleRelationResult.IviLoginResult.builder()
                        .state(IviLoginResultStatus.SUCCEEDED).reportedAt(BOUND_AT.plusSeconds(60)).build())
                .firstActivation(MptUserVehicleRelationResult.FirstActivationResult.builder()
                        .state(FirstActivationState.RECORDED).firstActivatedAt(BOUND_AT).build())
                .latestUnbind(MptUserVehicleRelationResult.UnbindInfoResult.builder()
                        .operationId("UBO-1").releasedAt(BOUND_AT.plusSeconds(120))
                        .credentialRevocationStatus(CredentialRevocationStatus.FAILED).build())
                .build();

        MptUserVehicleRelationVo vo = MptUserVehicleVoAssembler.INSTANCE.toVo(result, SHANGHAI);

        assertThat(vo.getRelation().getMaskedAccountId()).isEqualTo("acc*****01");
        assertThat(vo.getRelation().getMaskedAccountId()).doesNotContain("account-01");
        assertThat(vo.getRelation().getRowVersion()).isEqualTo(3);
        // UTC 02:00 → 上海 10:00，无偏移墙钟
        assertThat(vo.getRelation().getBoundAt()).isEqualTo(LocalDateTime.of(2026, 11, 25, 10, 0));
        assertThat(vo.getIviLogin().getReportedAt()).isEqualTo(LocalDateTime.of(2026, 11, 25, 10, 1));
        assertThat(vo.getFirstActivation().getState()).isEqualTo(FirstActivationState.RECORDED);
        assertThat(vo.getLatestUnbind().getCredentialRevocationStatus())
                .isEqualTo(CredentialRevocationStatus.FAILED);
    }
}
