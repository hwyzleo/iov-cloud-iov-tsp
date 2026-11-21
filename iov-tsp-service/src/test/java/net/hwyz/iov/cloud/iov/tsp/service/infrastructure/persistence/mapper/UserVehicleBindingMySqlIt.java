package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.api.vo.enums.PrivacyAuthorizationState;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.AccountVehiclePrivacyAuthorizationPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.ActivationFactPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.ActivationSessionPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.BindingOperationPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.UserVehicleRelationPo;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
 * 用户车辆关系真实 MySQL 集成测试（IOV-TSP-DSN-CR-017 TC017-04）。
 * <p>
 * 使用本机隔离容器 {@code tsp-cr014-mysql}（端口 13306）上的专用 schema {@code db_tsp_cr017}，
 * 先执行 V17 迁移建表，再以 MyBatis 注解 Mapper 直连验证：
 * <ol>
 *   <li>active_vin 生成列唯一键：每 VIN 至多一位当前主使用人，软删后允许新关系；</li>
 *   <li>会话状态/版本/到期条件更新（认领、取消、过期不提交）；</li>
 *   <li>首次激活候选事实业务键唯一（不产生第二业务事实）；</li>
 *   <li>隐私授权按来源版本收敛（旧版本不覆盖新状态）。</li>
 * </ol>
 * 类名以 It 结尾，surefire 默认不执行；DB 不可用时跳过。
 * <pre>
 *   mvn -o test -pl iov-tsp-service -Dtest=UserVehicleBindingMySqlIt
 * </pre>
 *
 * @author hwyz_leo
 */
class UserVehicleBindingMySqlIt {

    private static final String HOST = System.getProperty("cr017.db.host", "127.0.0.1");
    private static final String PORT = System.getProperty("cr017.db.port", "13306");
    private static final String SCHEMA = "db_tsp_cr017";
    private static final String USER = System.getProperty("cr017.db.user", "root");
    private static final String PASSWORD = System.getProperty("cr017.db.password", "cr014_root_pw");
    private static final String JDBC_PARAMS = "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";

    private static SqlSessionFactory sqlSessionFactory;
    private static boolean dbAvailable;

    @BeforeAll
    static void beforeAll() {
        String adminUrl = "jdbc:mysql://" + HOST + ":" + PORT + "/" + JDBC_PARAMS;
        try (Connection connection = DriverManager.getConnection(adminUrl, USER, PASSWORD)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE IF NOT EXISTS " + SCHEMA + " DEFAULT CHARSET utf8mb4");
            }
            try (Connection schemaConnection = DriverManager.getConnection(
                    "jdbc:mysql://" + HOST + ":" + PORT + "/" + SCHEMA + JDBC_PARAMS, USER, PASSWORD);
                 Statement statement = schemaConnection.createStatement()) {
                applyMigration(statement);
            }
            dbAvailable = true;
        } catch (Exception e) {
            dbAvailable = false;
            return;
        }
        Configuration configuration = new Configuration(new Environment("cr017-it",
                new JdbcTransactionFactory(),
                new PooledDataSource("com.mysql.cj.jdbc.Driver",
                        "jdbc:mysql://" + HOST + ":" + PORT + "/" + SCHEMA + JDBC_PARAMS, USER, PASSWORD)));
        configuration.setMapUnderscoreToCamelCase(false);
        configuration.addMapper(ActivationSessionMapper.class);
        configuration.addMapper(UserVehicleRelationMapper.class);
        configuration.addMapper(BindingOperationMapper.class);
        configuration.addMapper(ActivationFactMapper.class);
        configuration.addMapper(PrivacyAuthorizationMapper.class);
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    private static void applyMigration(Statement statement) throws IOException, java.sql.SQLException {
        Path migration = Path.of("src/main/resources/db/migration/V17__user_vehicle_binding.sql");
        String script = Files.readString(migration, StandardCharsets.UTF_8);
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

    private static void clean(String vinPrefix) {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            Connection connection = session.getConnection();
            try (Statement statement = connection.createStatement()) {
                statement.execute("DELETE FROM tb_user_vehicle_relation WHERE vin LIKE '" + vinPrefix + "%'");
                statement.execute("DELETE FROM tb_activation_session WHERE vin LIKE '" + vinPrefix + "%'");
                statement.execute("DELETE FROM tb_user_vehicle_binding_operation WHERE vin LIKE '" + vinPrefix + "%'");
                statement.execute("DELETE FROM tb_user_vehicle_activation_fact WHERE vin LIKE '" + vinPrefix + "%'");
                statement.execute("DELETE FROM tb_account_vehicle_privacy_authorization WHERE vin LIKE '" + vinPrefix + "%'");
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private ActivationSessionPo newSession(String sessionId, String qrHash, String vin, String state,
                                           LocalDateTime expiresAt) {
        ActivationSessionPo po = ActivationSessionPo.builder()
                .sessionId(sessionId).qrTokenHash(qrHash).vin(vin).state(state)
                .expiresAt(expiresAt).agreementRevisions("PRIVACY_POLICY:REV-P,USER_AGREEMENT:REV-U")
                .build();
        po.setRowVersion(1);
        po.setRowValid(true);
        return po;
    }

    @Test
    @DisplayName("active_vin 唯一键：同 VIN 至多一位当前主使用人，软删后可建立新关系")
    void activeVinUniqueConstraint() throws Exception {
        assumeDb();
        clean("ITVIN00000000001");
        String vin = "ITVIN00000000001";
        LocalDateTime now = LocalDateTime.parse("2026-11-20T10:00:00");
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            UserVehicleRelationMapper mapper = session.getMapper(UserVehicleRelationMapper.class);
            mapper.insert(UserVehicleRelationPo.builder().relationId("REL-IT-1").accountId("acc-1").vin(vin)
                    .role("PRIMARY_USER").state("ACTIVE").boundAt(now).sourceOperationId("BOP-IT-1").build());
            assertThatThrownBy(() -> mapper.insert(UserVehicleRelationPo.builder().relationId("REL-IT-2")
                    .accountId("acc-2").vin(vin).role("PRIMARY_USER").state("ACTIVE").boundAt(now)
                    .sourceOperationId("BOP-IT-2").build()))
                    .isInstanceOf(PersistenceException.class);
            // 软删旧关系后允许新账号建立当前关系
            session.getConnection().createStatement()
                    .execute("UPDATE tb_user_vehicle_relation SET row_valid = 0 WHERE relation_id = 'REL-IT-1'");
            mapper.insert(UserVehicleRelationPo.builder().relationId("REL-IT-3").accountId("acc-2").vin(vin)
                    .role("PRIMARY_USER").state("ACTIVE").boundAt(now).sourceOperationId("BOP-IT-3").build());
            assertThat(mapper.selectActiveByVin(vin).getRelationId()).isEqualTo("REL-IT-3");
        }
    }

    @Test
    @DisplayName("当前关系查询：仅返回本人 ACTIVE 且 row_valid=1 的关系，不泄露他人")
    void currentRelationQueryIsAccountIsolated() throws Exception {
        assumeDb();
        clean("ITVIN00000000006");
        String vin = "ITVIN00000000006";
        LocalDateTime now = LocalDateTime.parse("2026-11-21T10:00:00");
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            UserVehicleRelationMapper mapper = session.getMapper(UserVehicleRelationMapper.class);
            Statement statement = session.getConnection().createStatement();

            mapper.insert(UserVehicleRelationPo.builder().relationId("REL-IT-Q1").accountId("acc-1").vin(vin)
                    .role("PRIMARY_USER").state("ACTIVE").boundAt(now).sourceOperationId("BOP-IT-Q1").build());
            // 本人 ACTIVE 可查到；其他账号查不到，且不按 VIN 反查他人关系
            UserVehicleRelationPo own = mapper.selectActiveByAccountAndVin("acc-1", vin);
            assertThat(own).isNotNull();
            assertThat(own.getRelationId()).isEqualTo("REL-IT-Q1");
            assertThat(mapper.selectActiveByAccountAndVin("acc-2", vin)).isNull();

            // RELEASED 自然映射为无关系
            statement.execute("UPDATE tb_user_vehicle_relation SET state = 'RELEASED' WHERE relation_id = 'REL-IT-Q1'");
            session.clearCache();
            assertThat(mapper.selectActiveByAccountAndVin("acc-1", vin)).isNull();

            // 逻辑删除自然映射为无关系
            statement.execute("UPDATE tb_user_vehicle_relation SET state = 'ACTIVE', row_valid = 0 "
                    + "WHERE relation_id = 'REL-IT-Q1'");
            session.clearCache();
            assertThat(mapper.selectActiveByAccountAndVin("acc-1", vin)).isNull();

            // 仅存在他人有效关系时，当前账号仍为无关系
            mapper.insert(UserVehicleRelationPo.builder().relationId("REL-IT-Q2").accountId("acc-2").vin(vin)
                    .role("PRIMARY_USER").state("ACTIVE").boundAt(now).sourceOperationId("BOP-IT-Q2").build());
            assertThat(mapper.selectActiveByAccountAndVin("acc-1", vin)).isNull();
            assertThat(mapper.selectActiveByAccountAndVin("acc-2", vin).getRelationId()).isEqualTo("REL-IT-Q2");
        }
    }

    @Test
    @DisplayName("会话条件更新：版本不符/过期/已认领均拒绝，有效请求恰好成功一次")
    void sessionConditionalClaim() throws Exception {
        assumeDb();
        clean("ITVIN00000000002");
        String vin = "ITVIN00000000002";
        LocalDateTime now = LocalDateTime.parse("2026-11-20T10:00:00");
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            ActivationSessionMapper mapper = session.getMapper(ActivationSessionMapper.class);
            mapper.insert(newSession("SES-IT-1", "HASH-IT-1", vin, "ISSUED", now.plusMinutes(10)));

            // 版本不符
            assertThat(mapper.claim("SES-IT-1", "acc-1", now, 2)).isZero();
            // 正常认领
            assertThat(mapper.claim("SES-IT-1", "acc-1", now, 1)).isEqualTo(1);
            // 已认领后其他账号不可再认领（状态已变化）
            assertThat(mapper.claim("SES-IT-1", "acc-2", now, 2)).isZero();
            ActivationSessionPo claimed = mapper.selectBySessionId("SES-IT-1");
            assertThat(claimed.getState()).isEqualTo("SCANNED");
            assertThat(claimed.getClaimedAccountId()).isEqualTo("acc-1");

            // 过期会话不可认领
            mapper.insert(newSession("SES-IT-2", "HASH-IT-2", vin, "ISSUED", now.minusMinutes(1)));
            assertThat(mapper.claim("SES-IT-2", "acc-1", now, 1)).isZero();

            // 确认前过期不可提交绑定
            assertThat(mapper.markBound("SES-IT-1", "BOP-IT-1", now, now.plusMinutes(30), 2)).isEqualTo(1);
            assertThat(mapper.markBound("SES-IT-2", "BOP-IT-2", now, now.plusMinutes(30), 1)).isZero();
            assertThat(mapper.selectBySessionId("SES-IT-1").getState()).isEqualTo("BOUND");
        }
    }

    @Test
    @DisplayName("首次激活候选事实：同业务键只允许一条事实")
    void activationFactUniqueBusinessKey() throws Exception {
        assumeDb();
        clean("ITVIN00000000003");
        String vin = "ITVIN00000000003";
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            ActivationFactMapper mapper = session.getMapper(ActivationFactMapper.class);
            ActivationFactPo po = ActivationFactPo.builder().eventId("UVA-IT-1").schemaVersion(1).vin(vin)
                    .bindingOperationId("BOP-IT-1").sourceFactRef("order=ORDER-IT")
                    .occurredAt(LocalDateTime.parse("2026-11-20T10:00:00")).payloadDigest("digest-1")
                    .publishStatus("PENDING").build();
            mapper.insert(po);
            assertThatThrownBy(() -> mapper.insert(ActivationFactPo.builder().eventId("UVA-IT-2").schemaVersion(1)
                    .vin(vin).bindingOperationId("BOP-IT-1").occurredAt(LocalDateTime.parse("2026-11-20T10:01:00"))
                    .payloadDigest("digest-2").publishStatus("PENDING").build()))
                    .isInstanceOf(PersistenceException.class);
            List<ActivationFactPo> pending = mapper.selectPending(10);
            assertThat(pending).extracting(ActivationFactPo::getEventId).contains("UVA-IT-1");
        }
    }

    @Test
    @DisplayName("隐私授权按来源版本收敛：旧版本条件更新不生效")
    void privacyVersionConvergence() throws Exception {
        assumeDb();
        clean("ITVIN00000000004");
        String vin = "ITVIN00000000004";
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            PrivacyAuthorizationMapper mapper = session.getMapper(PrivacyAuthorizationMapper.class);
            LocalDateTime now = LocalDateTime.parse("2026-11-20T10:00:00");
            mapper.insert(AccountVehiclePrivacyAuthorizationPo.builder()
                    .authorizationId("PAZ-IT-1").accountId("acc-1").vin(vin).authorizationItem("LOCATION")
                    .state(PrivacyAuthorizationState.GRANTED.name()).version(5L).source("VEHICLE")
                    .reportId("RPT-1").payloadDigest("digest-1").reportedAt(now).receivedAt(now).build());
            // 版本更旧：不覆盖
            assertThat(mapper.updateIfNewer(AccountVehiclePrivacyAuthorizationPo.builder()
                    .authorizationId("PAZ-IT-1").accountId("acc-1").vin(vin).authorizationItem("LOCATION")
                    .state(PrivacyAuthorizationState.DENIED.name()).version(3L).source("VEHICLE")
                    .reportId("RPT-2").payloadDigest("digest-2").reportedAt(now).receivedAt(now).build())).isZero();
            assertThat(mapper.select("acc-1", vin, "LOCATION").getState())
                    .isEqualTo(PrivacyAuthorizationState.GRANTED.name());
            // 版本更新：覆盖
            assertThat(mapper.updateIfNewer(AccountVehiclePrivacyAuthorizationPo.builder()
                    .authorizationId("PAZ-IT-1").accountId("acc-1").vin(vin).authorizationItem("LOCATION")
                    .state(PrivacyAuthorizationState.DENIED.name()).version(6L).source("VEHICLE")
                    .reportId("RPT-3").payloadDigest("digest-3").reportedAt(now).receivedAt(now).build())).isEqualTo(1);
            assertThat(mapper.select("acc-1", vin, "LOCATION").getState())
                    .isEqualTo(PrivacyAuthorizationState.DENIED.name());
        }
    }

    @Test
    @DisplayName("绑定操作幂等键唯一：同作用域账号键只允许一条操作")
    void bindingOperationIdempotencyUnique() throws Exception {
        assumeDb();
        clean("ITVIN00000000005");
        String vin = "ITVIN00000000005";
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            BindingOperationMapper mapper = session.getMapper(BindingOperationMapper.class);
            BindingOperationPo po = BindingOperationPo.builder().operationId("BOP-IT-1").callerScope("APP")
                    .accountId("acc-1").idempotencyKey("idem-1").requestDigest("digest-1")
                    .sessionId("SES-IT-1").vin(vin).status("PENDING").build();
            mapper.insert(po);
            assertThatThrownBy(() -> mapper.insert(BindingOperationPo.builder().operationId("BOP-IT-2")
                    .callerScope("APP").accountId("acc-1").idempotencyKey("idem-1").requestDigest("digest-2")
                    .sessionId("SES-IT-1").vin(vin).status("PENDING").build()))
                    .isInstanceOf(PersistenceException.class);
            assertThat(mapper.selectByIdempotencyKey("APP", "acc-1", "idem-1").getOperationId())
                    .isEqualTo("BOP-IT-1");
        }
    }
}
