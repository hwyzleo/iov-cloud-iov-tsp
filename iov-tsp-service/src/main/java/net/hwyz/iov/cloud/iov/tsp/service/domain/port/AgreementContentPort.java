package net.hwyz.iov.cloud.iov.tsp.service.domain.port;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.activation.AgreementBundle;

/**
 * 协议内容端口（MKT-CMS 防腐层）。
 *
 * @author hwyz_leo
 */
public interface AgreementContentPort {

    /**
     * 读取适用语言下的当前发布包（固定两项协议）
     *
     * @param language 语言；null 表示服务端默认
     * @return 协议发布包
     * @throws DependencyUnavailableException 内容不可用（不伪造协议）
     */
    AgreementBundle loadCurrent(String language);
}
