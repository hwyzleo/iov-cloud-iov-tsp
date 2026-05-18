package net.hwyz.iov.cloud.iov.tsp.api.vo.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.CcpVo;

import java.util.List;

/**
 * 批量导入中央计算平台信息请求
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchImportCcpRequest {

    /**
     * 中央计算平台列表
     */
    @NotEmpty(message = "中央计算平台列表不能为空")
    private List<CcpVo> ccpList;

    /**
     * 供应商编码
     */
    private String supplierCode;

    /**
     * 批次号
     */
    private String batchNum;

}
