package net.hwyz.iov.cloud.tsp.mno.api.feign.mpt;

import jakarta.servlet.http.HttpServletResponse;
import net.hwyz.iov.cloud.framework.common.web.domain.AjaxResult;
import net.hwyz.iov.cloud.framework.common.web.page.TableDataInfo;
import net.hwyz.iov.cloud.tsp.mno.api.contract.SimMpt;

/**
 * SIM卡相关管理后台接口
 *
 * @author hwyz_leo
 */
public interface SimMptApi {

    /**
     * 分页查询SIM卡信息
     *
     * @param sim SIM卡信息
     * @return SIM卡信息列表
     */
    TableDataInfo list(SimMpt sim);

    /**
     * 导出SIM卡信息
     *
     * @param response 响应
     * @param sim      SIM卡信息
     */
    void export(HttpServletResponse response, SimMpt sim);

    /**
     * 根据SIM卡ID获取SIM卡信息
     *
     * @param simId SIM卡ID
     * @return SIM卡信息
     */
    AjaxResult getInfo(Long simId);

    /**
     * 新增SIM卡信息
     *
     * @param sim SIM卡信息
     * @return 结果
     */
    AjaxResult add(SimMpt sim);

    /**
     * 修改保存SIM卡信息
     *
     * @param sim SIM卡信息
     * @return 结果
     */
    AjaxResult edit(SimMpt sim);

    /**
     * 删除SIM卡信息
     *
     * @param simIds SIM卡ID数组
     * @return 结果
     */
    AjaxResult remove(Long[] simIds);

}
