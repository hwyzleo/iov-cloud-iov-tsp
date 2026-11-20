package net.hwyz.iov.cloud.iov.tsp.api.vo.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.AgreementType;

/**
 * 车端实际展示并明确同意的协议修订证据。
 * <p>
 * IOV-TSP-DSN-CR-017 D017-02/D017-06：只接受实际展示的不可变修订，不在服务端重新取「最新版」覆盖。
 *
 * @author hwyz_leo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgreementAcceptanceRequest {

    /**
     * 协议类型（隐私政策/用户协议各一项）
     */
    @NotNull(message = "协议类型不能为空")
    private AgreementType agreementType;

    /**
     * 用户实际同意（必须为 true，服务端不接受默认值推断）
     */
    @NotNull(message = "协议同意标记不能为空")
    private Boolean agreed;

    /**
     * 展示版本
     */
    @NotBlank(message = "协议版本不能为空")
    private String version;

    /**
     * CMS 不可变修订标识
    */
    @NotBlank(message = "协议修订标识不能为空")
    private String revisionId;

    /**
     * 协议语言
     */
    private String language;

    /**
     * 协议内容摘要（可选，用于同意证据比对）
     */
    private String contentHash;
}
