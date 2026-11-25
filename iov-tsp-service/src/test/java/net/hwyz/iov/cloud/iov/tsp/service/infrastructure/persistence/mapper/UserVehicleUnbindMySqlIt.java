package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.PrivacyAuthorizationState;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.AccountVehiclePrivacyAuthorizationPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.ActivationLoginTicketPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.UserVehicleHistoryPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.UserVehicleRelationPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.UserVehicleUnbindOperationPo;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 人工解绑真实 MySQL 集成测试（IOV-TSP-DSN-CR-021 TC021-04～TC021-13）。
 * <p>
 * 使用本机隔离容器 {@code tsp-cr014-mysql}（端口 13306）上的专用 schema {@code db_tsp_cr021}，
 * 先执行 V17/V18/V20 迁移，再以 MyBatis 注解/XML Mapper 直连验证：
 * <ol>
 *   <li>V20 增量列与 active_vin 释放后可重新建立 ACTIVE 关系；</li>
 *   <li>关系释放 CAS 与解绑台账唯一键/状态推进/重试候选；</li>
 *   <li>来源会话未消费票据 REVOKED，旧票据不可再消费；</li>
 *   <li>隐私授权按 Account+VIN+relationId+授权项唯一，历史不删除；</li>
 *   <li>专用历史查询一次取出关系与撤销状态。</li>
 * </ol>
 * 类名以 It 结尾，surefire 默认不执行；DB 不可用时跳过。
 * <pre>
 *   mvn -o test -pl iov-tsp-service -am -Dtest=UserVehicleUnbindMySqlIt
 * </pre>
 *
 * @author hwyz_leo
 */
class UserVehicleUnbindMySqlIt {

    private static final String HOST = System.getProperty("cr021.db.host", "127.0.0.1");
    private static final String PORT = System.getProperty("cr021.db.port", "13306");
    private static final String SCHEMA = "db_tsp_cr021";
    private static final String USER = System.getProperty("cr021.db.user", "root");
    private static final String PASSWORD = System.getProperty("cr021.db.password", "cr014_root_pw");
    private static final String JDBC_PARAMS =
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";
    private static final LocalDateTime NOW = LocalDateTime.parse("2026-11-25T10:00:00");

    private static SqlSessionFactory sqlSessionFactory;
    private static boolean dbAvailable;

    @BeforeAll
    static void beforeAll() throws Exception {
        String adminUrl = "jdbc:mysql://" + HOST + ":" + PORT + "/" + JDBC_PARAMS;
        try (Connection connection = DriverManager.getConnection(adminUrl, USER, PASSWORD)) {
            try (Statement statement = connection.createStatement()) {
                // 隔离 schema 每次重建，保证 V20 增量 ALTER 可重复执行
                statement.execute("DROP DATABASE IF EXISTS " + SCHEMA);
                statement.execute("CREATE DATABASE " + SCHEMA + " DEFAULT CHARSET utf8mb4");
            }
            try (Connection schemaConnection = DriverManager.getConnection(
                    "jdbc:mysql://" + HOST + ":" + PORT + "/" + SCHEMA + JDBC_PARAMS, USER, PASSWORD);
                 Statement statement = schemaConnection.createStatement()) {
                applyScript(statement, "db/migration/V17__user_vehicle_binding.sql");
                applyScript(statement, "db/migration/V18__activation_login_ticket.sql");
                applyScript(statement, "db/migration/V20__user_vehicle_unbind.sql");
            }
            dbAvailable = true;
        } catch (Exception e) {
            dbAvailable = false;
            return;
        }
        Configuration configuration = new Configuration(new Environment("cr021-it",
                new JdbcTransactionFactory(),
                new PooledDataSource("com.mysql.cj.jdbc.Driver",
                        "jdbc:mysql://" + HOST + ":" + PORT + "/" + SCHEMA + JDBC_PARAMS, USER, PASSWORD)));
        configuration.setMapUnderscoreToCamelCase(false);
        configuration.addMapper(UserVehicleRelationMapper.class);
        configuration.addMapper(UserVehicleUnbindOperationMapper.class);
        configuration.addMapper(ActivationLoginTicketMapper.class);
        configuration.addMapper(PrivacyAuthorizationMapper.class);
        configuration.addMapper(MptUserVehicleQueryMapper.class);
        try (InputStream in = Resources.getResourceAsStream("mappers/ActivationLoginTicketMapper.xml")) {
            new XMLMapperBuilder(in, configuration, "mappers/ActivationLoginTicketMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    private static void applyScript(Statement statement, String classpath) throws Exception {
        Path path = Path.of("src/main/resources/" + classpath);
        String script = Files.readString(path, StandardCharsets.UTF_8);
        StringBuilder cleaned = new StringBuilder();
        for (String line : script.split("\n")) {
            if (line.trim().startsWith("--")) {
                continue;
            }
            cleaned.append(line).append('\n');
        }
        for (String sql : cleaned.toString().split(";")) {
            if (!sql.isBlank()) {
                statement.execute(sql);
            }
        }
    }

    private static void assumeDb() {
        Assumptions.assumeTrue(dbAvailable, "本地 MySQL 隔离库不可用，跳过真实 DB 集成测试");
    }

    @BeforeEach
    void clean() {
        assumeDb();
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            try (Statement statement = session.getConnection().createStatement()) {
                statement.execute("DELETE FROM tb_user_vehicle_audit");
                statement.execute("DELETE FROM tb_user_vehicle_unbind_operation");
                statement.execute("DELETE FROM tb_activation_login_ticket");
                statement.execute("DELETE FROM tb_account_vehicle_privacy_authorization");
                statement.execute("DELETE FROM tb_user_vehicle_relation");
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private UserVehicleRelationPo relation(String relationId, String vin, String state, int rowVersion) {
        return UserVehicleRelationPo.builder()
                .relationId(relationId).accountId("acc-1").vin(vin).role("PRIMARY_USER").state(state)
                .boundAt(NOW.minusHours(2)).sourceOperationId("BOP-1").rowVersion(rowVersion).build();
    }

    @Test
    @DisplayName("V20：释放 CAS 与 active_vin 释放后可重新建立 ACTIVE 关系")
    void releaseCasAndRebind() {
        String vin = "IT21VIN0000000001";
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            UserVehicleRelationMapper relationMapper = session.getMapper(UserVehicleRelationMapper.class);
            UserVehicleUnbindOperationMapper opMapper = session.getMapper(UserVehicleUnbindOperationMapper.class);
            relationMapper.insert(relation("REL-21-1", vin, "ACTIVE", 1));
            // 过期版本：CAS 不命中
            assertThat(relationMapper.release("REL-21-1", 2, NOW, "CUSTOMER_VERIFIED_REQUEST", "UBO-21-1",
                    "mpt-admin", NOW)).isZero();
            assertThat(relationMapper.release("REL-21-1", 1, NOW, "CUSTOMER_VERIFIED_REQUEST", "UBO-21-1",
                    "mpt-admin", NOW)).isEqualTo(1);
            UserVehicleRelationPo released = relationMapper.selectByRelationId("REL-21-1");
            assertThat(released.getState()).isEqualTo("RELEASED");
            assertThat(released.getReleaseReason()).isEqualTo("CUSTOMER_VERIFIED_REQUEST");
            assertThat(released.getReleaseOperationId()).isEqualTo("UBO-21-1");
            assertThat(released.getReleasedBy()).isEqualTo("mpt-admin");
            assertThat(released.getReleasedAt()).isEqualTo(NOW);
            // active_vin 已释放：允许建立新的 ACTIVE 关系
            relationMapper.insert(relation("REL-21-2", vin, "ACTIVE", 1));
            assertThat(relationMapper.selectActiveByVin(vin).getRelationId()).isEqualTo("REL-21-2");
            assertThat(relationMapper.selectLatestReleasedByVin(vin).getRelationId()).isEqualTo("REL-21-1");

            // 专用历史查询一次取出关系与撤销状态
            opMapper.insert(UserVehicleUnbindOperationPo.builder()
                    .operationId("UBO-21-1").relationId("REL-21-1").accountId("acc-1").vin(vin)
                    .operatorId("mpt-admin").idempotencyKey("idem-21-1").requestDigest("d".repeat(64))
                    .expectedRowVersion(1).reason("CUSTOMER_VERIFIED_REQUEST")
                    .relationState("PENDING").credentialRevocationStatus("PENDING").build());
            assertThat(opMapper.markReleased("UBO-21-1", NOW, NOW, NOW.plusSeconds(30), NOW, 1)).isEqualTo(1);
            assertThat(opMapper.markRevocationFailed("UBO-21-1", "timeout", NOW.plusSeconds(60), 1, NOW, 2))
                    .isEqualTo(1);
            MptUserVehicleQueryMapper queryMapper = session.getMapper(MptUserVehicleQueryMapper.class);
            List<UserVehicleHistoryPo> history = queryMapper.selectHistory(vin, null, null);
            assertThat(history).hasSize(2);
            // 条件过滤在 SQL 侧完成
            assertThat(queryMapper.selectHistory(vin, "REL-21-1", null)).hasSize(1);
            assertThat(queryMapper.selectHistory(vin, null, "UBO-21-1")).hasSize(1);
            assertThat(queryMapper.selectHistory(vin, "REL-21-2", "UBO-21-1")).isEmpty();
            assertThat(history.stream()
                    .filter(item -> "REL-21-1".equals(item.getRelationId()))
                    .findFirst()
                    .orElseThrow()
                    .getCredentialRevocationStatus()).isEqualTo("FAILED");
        }
    }

    @Test
    @DisplayName("解绑台账：唯一键、撤销状态推进与重试候选")
    void unbindOperationIdempotencyAndRetry() {
        String vin = "IT21VIN0000000002";
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            UserVehicleUnbindOperationMapper mapper = session.getMapper(UserVehicleUnbindOperationMapper.class);
            UserVehicleUnbindOperationPo po = UserVehicleUnbindOperationPo.builder()
                    .operationId("UBO-21-2").relationId("REL-21-3").accountId("acc-1").vin(vin)
                    .operatorId("mpt-admin").idempotencyKey("idem-21-2").requestDigest("e".repeat(64))
                    .expectedRowVersion(3).reason("CUSTOMER_VERIFIED_REQUEST")
                    .relationState("PENDING").credentialRevocationStatus("PENDING").build();
            mapper.insert(po);
            assertThatThrownBy(() -> mapper.insert(UserVehicleUnbindOperationPo.builder()
                    .operationId("UBO-21-3").relationId("REL-21-3").accountId("acc-1").vin(vin)
                    .operatorId("mpt-admin").idempotencyKey("idem-21-2").requestDigest("f".repeat(64))
                    .expectedRowVersion(3).reason("CUSTOMER_VERIFIED_REQUEST")
                    .relationState("PENDING").credentialRevocationStatus("PENDING").build()))
                    .isInstanceOf(PersistenceException.class);
            assertThat(mapper.markReleased("UBO-21-2", NOW, NOW, NOW.plusSeconds(30), NOW, 1)).isEqualTo(1);
            assertThat(mapper.selectRevocationRetryCandidates(NOW, 10, 10)).isEmpty();
            assertThat(mapper.selectRevocationRetryCandidates(NOW.plusSeconds(30), 10, 10)).hasSize(1);

            assertThat(mapper.markRevocationSucceeded("UBO-21-2", 2, NOW.plusSeconds(31), 2)).isEqualTo(1);
            assertThat(mapper.selectByOperationId("UBO-21-2").getCredentialRevocationStatus())
                    .isEqualTo("SUCCEEDED");
            assertThat(mapper.selectByOperationId("UBO-21-2").getRevokedSessionCount()).isEqualTo(2);
            assertThat(mapper.selectRevocationRetryCandidates(NOW.plusSeconds(60), 10, 10)).isEmpty();
            // 已 SUCCEEDED 不再允许重复推进
            assertThat(mapper.markRevocationFailed("UBO-21-2", "late", NOW.plusSeconds(90), 1, NOW, 3)).isZero();
        }
    }

    @Test
    @DisplayName("票据：按来源会话作废只影响 ISSUED，旧票据不可再消费")
    void revokeIssuedTicketsBySession() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            ActivationLoginTicketMapper mapper = session.getMapper(ActivationLoginTicketMapper.class);
            mapper.insertPo(ActivationLoginTicketPo.builder()
                    .ticketId("TKT-21-1").ticketHash("h1").sessionId("SES-21-1").bindingOperationId("BOP-21-1")
                    .vin("IT21VIN0000000003").accountId("acc-1").relationId("REL-21-4").state("ISSUED")
                    .issuedAt(NOW.minusSeconds(10)).expiresAt(NOW.plusSeconds(170)).issueSeq(1).build());
            mapper.insertPo(ActivationLoginTicketPo.builder()
                    .ticketId("TKT-21-2").ticketHash("h2").sessionId("SES-21-1").bindingOperationId("BOP-21-1")
                    .vin("IT21VIN0000000003").accountId("acc-1").relationId("REL-21-4").state("CONSUMED")
                    .issuedAt(NOW.minusSeconds(20)).expiresAt(NOW.plusSeconds(160)).issueSeq(2).build());

            assertThat(mapper.revokeIssuedBySession("SES-21-1", NOW)).isEqualTo(1);
            assertThat(mapper.selectByTicketHash("h1").getState()).isEqualTo("REVOKED");
            assertThat(mapper.selectByTicketHash("h2").getState()).isEqualTo("CONSUMED");
            assertThat(mapper.consume("h1", "sec-ciam", NOW)).isZero();
            assertThat(mapper.revokeIssuedBySession("SES-21-1", NOW)).isZero();
        }
    }

    @Test
    @DisplayName("隐私授权：同关系周期唯一，不同关系周期并存，历史不删除")
    void privacyAuthorizationPerRelationCycle() {
        String vin = "IT21VIN0000000004";
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            PrivacyAuthorizationMapper mapper = session.getMapper(PrivacyAuthorizationMapper.class);
            mapper.insert(privacy("PAZ-21-1", vin, "REL-21-5", "LOCATION", PrivacyAuthorizationState.GRANTED, 1L));
            assertThatThrownBy(() -> mapper.insert(
                    privacy("PAZ-21-2", vin, "REL-21-5", "LOCATION", PrivacyAuthorizationState.DENIED, 2L)))
                    .isInstanceOf(PersistenceException.class);
            // 新关系周期可重新授权，旧周期不被覆盖
            mapper.insert(privacy("PAZ-21-3", vin, "REL-21-6", "LOCATION", PrivacyAuthorizationState.NOT_GRANTED, 1L));
            assertThat(mapper.select("acc-1", vin, "REL-21-5", "LOCATION").getState())
                    .isEqualTo(PrivacyAuthorizationState.GRANTED.name());
            assertThat(mapper.select("acc-1", vin, "REL-21-6", "LOCATION").getState())
                    .isEqualTo(PrivacyAuthorizationState.NOT_GRANTED.name());
            // 不同关系周期互不影响版本推进
            assertThat(mapper.updateIfNewer(
                    privacy("PAZ-21-1", vin, "REL-21-5", "LOCATION", PrivacyAuthorizationState.DENIED, 2L)))
                    .isEqualTo(1);
            assertThat(mapper.select("acc-1", vin, "REL-21-6", "LOCATION").getVersion()).isEqualTo(1L);
        }
    }

    private AccountVehiclePrivacyAuthorizationPo privacy(String id, String vin, String relationId, String item,
                                                         PrivacyAuthorizationState state, long version) {
        return AccountVehiclePrivacyAuthorizationPo.builder()
                .authorizationId(id).accountId("acc-1").vin(vin).relationId(relationId)
                .authorizationItem(item).state(state.name()).version(version).source("VEHICLE")
                .reportId("RPT-" + id).payloadDigest("digest-" + id)
                .reportedAt(NOW).receivedAt(NOW).build();
    }
}
