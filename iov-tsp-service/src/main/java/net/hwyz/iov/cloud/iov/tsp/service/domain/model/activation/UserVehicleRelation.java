package net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 用户—车辆主使用人关系（服务使用关系，不是法律产权）。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVehicleRelation {

    private Long id;

    /**
     * 关系业务ID
     */
    private String relationId;

    /**
     * 账号标识
     */
    private String accountId;

    /**
     * 车辆 VIN
     */
    private String vin;

    /**
     * 关系角色（本期 PRIMARY_USER）
     */
    private String role;

    /**
     * 关系状态 ACTIVE/RELEASED
     */
    private String state;

    /**
     * 绑定成功时刻
     */
    private Instant boundAt;

    /**
     * 来源绑定操作ID
     */
    private String sourceOperationId;

    /**
     * 资格规则版本
     */
    private String ruleVersion;

    /**
     * 事实引用摘要
     */
    private String factRef;

    /**
     * 乐观版本
     */
    private Integer rowVersion;
}
