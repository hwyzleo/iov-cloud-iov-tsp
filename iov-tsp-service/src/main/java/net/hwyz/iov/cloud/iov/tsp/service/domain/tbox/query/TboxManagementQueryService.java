package net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.query;

import lombok.RequiredArgsConstructor;
import net.hwyz.iov.cloud.framework.common.exception.BusinessException;
import net.hwyz.iov.cloud.iov.tsp.service.common.exception.MptTboxErrorCode;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxManagementQueryRepository;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementCriteria;
import net.hwyz.iov.cloud.iov.tsp.service.domain.tbox.model.TboxManagementView;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * TBOX 后台运营查询领域服务（只读）
 * <p>
 * TSP-DSN-CR-009：仅执行本地设备与 VMD 绑定投影聚合查询，不获取生命周期写锁，不调用外部服务。
 *
 * @author hwyz_leo
 */
@Service
@RequiredArgsConstructor
public class TboxManagementQueryService {

    private final TboxManagementQueryRepository tboxManagementQueryRepository;

    /**
     * 按条件查询聚合视图（分页由调用方通过 PageHelper 控制）
     *
     * @param criteria 查询条件
     * @return 聚合视图列表
     */
    public List<TboxManagementView> search(TboxManagementCriteria criteria) {
        return tboxManagementQueryRepository.search(criteria);
    }

    /**
     * 按 SN 获取聚合视图，不存在时抛出稳定 NOT_FOUND 语义
     *
     * @param sn 设备序列号
     * @return 聚合视图
     */
    public TboxManagementView getBySn(String sn) {
        TboxManagementView view = tboxManagementQueryRepository.getBySn(sn);
        if (view == null) {
            throw new BusinessException(MptTboxErrorCode.TBOX_NOT_FOUND);
        }
        return view;
    }
}
