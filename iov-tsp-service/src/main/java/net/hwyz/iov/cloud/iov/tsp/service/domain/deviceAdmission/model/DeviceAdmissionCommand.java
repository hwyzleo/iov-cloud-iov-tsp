package net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model;

import java.time.Instant;

/**
 * 设备接入准入裁决领域命令（TSP-DSN-CR-011 §3/§4.2）
 *
 * @param uid                设备接入身份（证书 CN = HSM UID）
 * @param certSerial         证书序列号
 * @param certFingerprint    证书 SHA-256 指纹
 * @param leafCertificateDer 本次 mTLS 实际叶子证书 DER
 * @param clientId           MQTT 客户端 ID
 * @param sourceIp           来源 IP（仅审计摘要）
 * @param caller             调用方标识（审计）
 * @param occurredAt         请求时刻（缺省为裁决时刻）
 * @author hwyz_leo
 */
public record DeviceAdmissionCommand(
        String uid,
        String certSerial,
        String certFingerprint,
        byte[] leafCertificateDer,
        String clientId,
        String sourceIp,
        String caller,
        Instant occurredAt) {
}
