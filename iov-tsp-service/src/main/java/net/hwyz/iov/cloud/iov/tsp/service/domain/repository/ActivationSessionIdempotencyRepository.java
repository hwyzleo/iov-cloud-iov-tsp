package net.hwyz.iov.cloud.iov.tsp.service.domain.repository;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.BindingCallerScope;
import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.ActivationSessionIdempotency;

import java.util.Optional;

/**
 * 车端会话创建幂等台账仓储（IOV-TSP-DSN-CR-020 D020-04/D020-05）。
 * <p>
 * 唯一键 {@code (caller_scope, vin, idempotency_key)} 是并发最终防线：
 * 同键并发插入抛 {@link org.springframework.dao.DuplicateKeyException}，由应用服务重新按台账判定。
 *
 * @author hwyz_leo
 */
public interface ActivationSessionIdempotencyRepository {

    /**
     * 按幂等作用域查询台账
     *
     * @param callerScope    调用方作用域（本期固定 VEHICLE）
     * @param vin            车辆 VIN
     * @param idempotencyKey 幂等键
     * @return 台账记录
     */
    Optional<ActivationSessionIdempotency> find(BindingCallerScope callerScope, String vin, String idempotencyKey);

    /**
     * 新增台账（唯一键冲突抛 DuplicateKeyException）
     *
     * @param record 台账记录
     */
    void insert(ActivationSessionIdempotency record);

    /**
     * 物理删除已释放（过期）台账，使同一幂等键可复用
     *
     * @param id 台账主键
     * @return 删除行数
     */
    int deleteById(Long id);
}
