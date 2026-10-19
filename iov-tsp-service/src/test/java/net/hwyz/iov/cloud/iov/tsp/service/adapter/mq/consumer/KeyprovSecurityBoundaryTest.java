package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TSP-DSN-CR-012 安全与边界护栏扫描（§10-7/§10-8）
 * <p>
 * ① 消费链路不依赖 Keyprov 协议/验签实现：无 proto-keyprov、ImportProofVerifier、
 * P1363/ECDSA 实现或 VMD Keyprov Client；<br>
 * ② 日志/指标/DLT header 不输出 keyId 或密码学材料。<br>
 * 仅静态扫描生产代码（src/main），避免把测试断言文本误判为违规。
 */
class KeyprovSecurityBoundaryTest {

    private static final Path MAIN_JAVA = Path.of("src/main/java");
    private static final Path MAIN_RESOURCES = Path.of("src/main/resources");

    private static final List<String> FORBIDDEN_DEPENDENCIES = List.of(
            "proto-keyprov",
            "ImportProofVerifier",
            "P1363",
            "ECDSA",
            "VmdKeyprovClient",
            "KeyProvisioningClient",
            "WrappedKey",
            "kmsKeyRef");

    private static final Pattern LOG_WITH_KEY_ID =
            Pattern.compile("log\\.(trace|debug|info|warn|error)\\([^;]*getKeyId\\(\\)");
    private static final Pattern LOG_WITH_RAW_VALUE =
            Pattern.compile("log\\.(trace|debug|info|warn|error)\\([^;]*record\\.value\\(\\)");
    private static final Pattern METRIC_TAG_WITH_IDENTITY =
            Pattern.compile("\\.tag\\(\\s*\"(deviceSn|requestId|keyId|vin|digest|contractDigest)\"");
    private static final Pattern DLT_KEY_ID_HEADER =
            Pattern.compile("put\\(\\s*headers\\s*,\\s*\"(keyId|key_id|proof|certificateDigest)\"");

    @Test
    void consumerSources_ShouldNotDependOnKeyprovCryptoImplementation() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaSources()) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            for (String token : FORBIDDEN_DEPENDENCIES) {
                if (content.contains(token)) {
                    violations.add(file + " -> " + token);
                }
            }
        }
        assertTrue(violations.isEmpty(), "V2C 消费链路引入了 Keyprov/密码学实现依赖: " + violations);
    }

    @Test
    void sourceCode_ShouldNotLogKeyIdOrRawPayload() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : allSources()) {
            if (!Files.isRegularFile(file)) {
                continue;
            }
            String content = Files.readString(file, StandardCharsets.UTF_8);
            if (LOG_WITH_KEY_ID.matcher(content).find()) {
                violations.add(file + " -> 日志输出 keyId");
            }
            if (LOG_WITH_RAW_VALUE.matcher(content).find()) {
                violations.add(file + " -> 日志输出原始 payload");
            }
        }
        assertTrue(violations.isEmpty(), "日志禁止输出 keyId 或原始 payload: " + violations);
    }

    @Test
    void metrics_ShouldNotUseIdentityOrDigestTags() throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaSources()) {
            String content = Files.readString(file, StandardCharsets.UTF_8);
            if (!content.contains("tsp_keyprov")) {
                continue;
            }
            if (METRIC_TAG_WITH_IDENTITY.matcher(content).find()) {
                violations.add(file.toString());
            }
        }
        assertTrue(violations.isEmpty(), "指标 label 禁止包含 deviceSn/requestId/keyId/VIN/摘要: " + violations);
    }

    @Test
    void dltPublisher_ShouldNotAddKeyIdHeader() throws IOException {
        Path publisher = Path.of("src/main/java/net/hwyz/iov/cloud/iov/tsp/service/adapter/mq/consumer/support/"
                + "TboxV2cKeyProvisionedDltPublisher.java");
        String content = Files.readString(publisher, StandardCharsets.UTF_8);
        assertTrue(!DLT_KEY_ID_HEADER.matcher(content).find(),
                "DLT 发布器不得新增 keyId/proof/证书摘要 header");
    }

    private List<Path> javaSources() throws IOException {
        try (Stream<Path> stream = Files.walk(MAIN_JAVA)) {
            return stream.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private List<Path> allSources() throws IOException {
        List<Path> paths = new ArrayList<>(javaSources());
        if (Files.exists(MAIN_RESOURCES)) {
            try (Stream<Path> stream = Files.walk(MAIN_RESOURCES)) {
                stream.filter(Files::isRegularFile).forEach(paths::add);
            }
        }
        return paths;
    }
}
