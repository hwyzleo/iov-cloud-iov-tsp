package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import net.hwyz.iov.cloud.framework.mysql.po.BasePo;

import java.time.LocalDateTime;

/**
 * 用户—车辆主使用人关系数据对象。
 *
 * @author hwyz_leo
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserVehicleRelationPo extends BasePo {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String relationId;

    private String accountId;

    private String vin;

    private String role;

    private String state;

    private LocalDateTime boundAt;

    private String sourceOperationId;

    private String ruleVersion;

    private String factRef;
}
