package net.hwyz.iov.cloud.iov.tsp.api.vo.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * MPT 人工解绑请求（IOV-TSP-DSN-CR-021 D021-03）。
 * <p>
 * 必须同时提供预期 rowVersion（防过期页面误释放）、原因码与幂等键；
 * 幂等键字符集与既有绑车确认保持一致，存在但非法时按 401112 拒绝。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MptUserVehicleUnbindRequest {

    /**
     * 预期关系版本（页面读取时的 rowVersion）
     */
    @NotNull(message = "关系版本不能为空")
    @Min(value = 1, message = "关系版本必须大于等于 1")
    private Integer rowVersion;

    /**
     * 解绑原因码（非空，最大 64；长摘要不进入原因列）
     */
    @NotBlank(message = "解绑原因不能为空")
    @Size(max = 64, message = "解绑原因长度不能超过 64")
    private String reason;

    /**
     * 幂等键（同一操作人下唯一，1..128）
     */
    @NotBlank(message = "幂等键不能为空")
    @Size(max = 128, message = "幂等键长度不能超过 128")
    @Pattern(regexp = "^[A-Za-z0-9_.:-]{1,128}$", message = "幂等键格式非法")
    private String idempotencyKey;
}
