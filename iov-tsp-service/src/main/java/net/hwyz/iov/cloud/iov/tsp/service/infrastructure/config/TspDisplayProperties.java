package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * TSP 展示时区配置（TSP-DSN-CR-014）。
 * <p>
 * 存储/传输始终为 UTC（JVM UTC、DB UTC 墙钟、JSON epoch 输出 zone-id=UTC）；
 * 本配置仅用于<b>后端展示层</b>（如 MPT Excel 导出、运营格式化）把 UTC 墙钟
 * 转换为目标时区墙钟显示，默认中国时区 {@code Asia/Shanghai}。
 * JSON API 输出 epoch 数字（绝对时刻），展示时区由前端自行转换，不受本配置影响。
 *
 * @author hwyz_leo
 * @since 2026-10-31
 */
@Data
@Component
@ConfigurationProperties(prefix = "tsp.display")
public class TspDisplayProperties {

    /**
     * 展示时区 ID（IANA），默认 Asia/Shanghai
     */
    private String zoneId = "Asia/Shanghai";

    /**
     * 展示时区对象；非法值时回退默认 Asia/Shanghai
     *
     * @return 展示时区
     */
    public ZoneId zone() {
        try {
            return ZoneId.of(zoneId);
        } catch (Exception e) {
            return ZoneId.of("Asia/Shanghai");
        }
    }
}
