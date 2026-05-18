package net.hwyz.iov.cloud.iov.tsp.api.vo.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.SimVo;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.MnoType;

import java.util.List;

/**
 * 批量导入SIM卡信息请求
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchImportSimRequest {

    /**
     * 运营商类型
     */
    @NotNull(message = "运营商类型不能为空")
    private MnoType mnoType;

    /**
     * SIM卡列表
     */
    @NotEmpty(message = "SIM卡列表不能为空")
    private List<SimVo> simList;

    /**
     * 批次号
     */
    private String batchNum;

}
