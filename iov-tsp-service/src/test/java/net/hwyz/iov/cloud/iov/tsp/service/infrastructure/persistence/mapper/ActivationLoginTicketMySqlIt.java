package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.ActivationLoginTicketPo;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 一次性登录票据真实 MySQL 集成测试（IOV-TSP-DSN-CR-019 TC019-03/TC019-05/TC019-09）。
 * <p>
 * 使用本机隔离容器 {@code tsp-cr014-mysql}（端口 13306）上的专用 schema {@code db_tsp_cr019}，
 * 先执行 V18 迁移建表，再以 MyBatis（XML 条件更新）直连验证：
 * <ol>
 *   <li>并发消费仅一次成功，其余影响行数为 0（数据库条件更新作为唯一防线）；</li>
 *   <li>过期票据不可消费；</li>
 *   <li>按会话作废只影响 ISSUED 票据；</li>
 *   <li>票据摘要/业务标识唯一；issue_seq 反映重签计数。</li>
 * </ol>
 * 类名以 It 结尾，surefire 默认不执行；DB 不可用时跳过。
 * <pre>
 *   mvn -o test -pl iov-tsp-service -am -Dtest=ActivationLoginTicketMySqlIt
 * </pre>
 *
 * @author hwyz_leo
 */
class ActivationLoginTicketMySqlIt {

    private static final String HOST = System.getProperty("cr019.db.host", "127.0.0.1");
    private static final String PORT = System.getProperty("cr019.db.port", "13306");
    private static final String SCHEMA = "db_tsp_cr019";
    private static final String USER = System.getProperty("cr019.db.user", "root");
    private static final String PASSWORD = System.getProperty("cr019.db.password", "cr014_root_pw");
    private static final String JDBC_PARAMS =
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";

    private static SqlSessionFactory sqlSessionFactory;
    private static boolean dbAvailable;

    @BeforeAll
    static void beforeAll() throws Exception {
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
        Configuration configuration = new Configuration(new Environment("cr019-it",
                new JdbcTransactionFactory(),
                new PooledDataSource("com.mysql.cj.jdbc.Driver",
                        "jdbc:mysql://" + HOST + ":" + PORT + "/" + SCHEMA + JDBC_PARAMS, USER, PASSWORD)));
        configuration.setMapUnderscoreToCamelCase(false);
        configuration.addMapper(ActivationLoginTicketMapper.class);
        try (InputStream in = Resources.getResourceAsStream("mappers/ActivationLoginTicketMapper.xml")) {
            new XMLMapperBuilder(in, configuration, "mappers/ActivationLoginTicketMapper.xml",
                    configuration.getSqlFragments()).parse();
        }
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    @BeforeEach
    void clean() {
        assumeDb();
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            try (Statement statement = session.getConnection().createStatement()) {
                statement.execute("DELETE FROM tb_activation_login_ticket");
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void applyMigration(Statement statement) throws IOException, java.sql.SQLException {
        Path migration = Path.of("src/main/resources/db/migration/V18__activation_login_ticket.sql");
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

    private static LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private ActivationLoginTicketPo ticket(String ticketId, String hash, String sessionId, String state,
                                           LocalDateTime expiresAt, int issueSeq, String consumedBy) {
        return ActivationLoginTicketPo.builder()
                .ticketId(ticketId)
                .ticketHash(hash)
                .sessionId(sessionId)
                .bindingOperationId("BOP-1")
                .vin("LSVAA1234567890AB")
                .accountId("account-1")
                .relationId("REL-1")
                .state(state)
                .issuedAt(utcNow().minusSeconds(1))
                .expiresAt(expiresAt)
                .consumedBy(consumedBy)
                .issueSeq(issueSeq)
                .build();
    }

    @Test
    @DisplayName("并发消费同一票据：数据库条件更新保证仅一次成功")
    void concurrentConsumeOnlyOnce() throws Exception {
        String hash = "a".repeat(64);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.getMapper(ActivationLoginTicketMapper.class)
                    .insertPo(ticket("TKT-1", hash, "SES-1", "ISSUED", utcNow().plusSeconds(180), 1, null));
        }
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        List<Throwable> failures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            final int index = i;
            pool.submit(() -> {
                try {
                    start.await();
                    try (SqlSession session = sqlSessionFactory.openSession(true)) {
                        int affected = session.getMapper(ActivationLoginTicketMapper.class)
                                .consume(hash, "sec-ciam-" + index, utcNow());
                        if (affected == 1) {
                            successes.incrementAndGet();
                        }
                    }
                } catch (Throwable t) {
                    synchronized (failures) {
                        failures.add(t);
                    }
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        assertThat(failures).isEmpty();
        assertThat(successes.get()).isEqualTo(1);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            ActivationLoginTicketPo stored = session.getMapper(ActivationLoginTicketMapper.class)
                    .selectByTicketHash(hash);
            assertThat(stored.getState()).isEqualTo("CONSUMED");
            assertThat(stored.getConsumedBy()).startsWith("sec-ciam-");
            assertThat(stored.getConsumedAt()).isNotNull();
        }
    }

    @Test
    @DisplayName("过期票据：条件更新影响行数为 0，保持 ISSUED 不变")
    void expiredTicketCannotBeConsumed() {
        String hash = "b".repeat(64);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.getMapper(ActivationLoginTicketMapper.class).insertPo(
                    ticket("TKT-2", hash, "SES-2", "ISSUED", utcNow().minusSeconds(1), 1, null));
        }
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            int affected = session.getMapper(ActivationLoginTicketMapper.class)
                    .consume(hash, "sec-ciam", utcNow().plusSeconds(1));
            assertThat(affected).isZero();
            assertThat(session.getMapper(ActivationLoginTicketMapper.class)
                    .selectByTicketHash(hash).getState()).isEqualTo("ISSUED");
        }
    }

    @Test
    @DisplayName("按会话作废：只把 ISSUED 置 REVOKED，已消费票据不变")
    void revokeOnlyIssuedTickets() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            ActivationLoginTicketMapper mapper = session.getMapper(ActivationLoginTicketMapper.class);
            mapper.insertPo(ticket("TKT-3", "c".repeat(64), "SES-3", "ISSUED", utcNow().plusSeconds(180), 1, null));
            mapper.insertPo(ticket("TKT-4", "d".repeat(64), "SES-3", "ISSUED", utcNow().plusSeconds(180), 2, null));
            mapper.insertPo(ticket("TKT-5", "e".repeat(64), "SES-3", "CONSUMED", utcNow().plusSeconds(180), 3,
                    "sec-ciam"));
        }
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            ActivationLoginTicketMapper mapper = session.getMapper(ActivationLoginTicketMapper.class);
            assertThat(mapper.revokeIssuedBySession("SES-3", utcNow())).isEqualTo(2);
            assertThat(mapper.selectByTicketHash("c".repeat(64)).getState()).isEqualTo("REVOKED");
            assertThat(mapper.selectByTicketHash("d".repeat(64)).getState()).isEqualTo("REVOKED");
            assertThat(mapper.selectByTicketHash("e".repeat(64)).getState()).isEqualTo("CONSUMED");
            // 重复作废无副作用
            assertThat(mapper.revokeIssuedBySession("SES-3", utcNow())).isZero();
        }
    }

    @Test
    @DisplayName("票据摘要唯一：同摘要重复写入被数据库拒绝")
    void ticketHashIsUnique() {
        String hash = "f".repeat(64);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.getMapper(ActivationLoginTicketMapper.class)
                    .insertPo(ticket("TKT-6", hash, "SES-4", "ISSUED", utcNow().plusSeconds(180), 1, null));
        }
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            assertThatThrownBy(() -> session.getMapper(ActivationLoginTicketMapper.class)
                    .insertPo(ticket("TKT-7", hash, "SES-4", "ISSUED", utcNow().plusSeconds(180), 2, null)))
                    .isInstanceOf(PersistenceException.class);
        }
    }

    @Test
    @DisplayName("签发序号：按会话返回最大 issue_seq，重签计数可续")
    void maxIssueSeqPerSession() {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            ActivationLoginTicketMapper mapper = session.getMapper(ActivationLoginTicketMapper.class);
            mapper.insertPo(ticket("TKT-8", "1".repeat(64), "SES-5", "REVOKED", utcNow(), 1, null));
            mapper.insertPo(ticket("TKT-9", "2".repeat(64), "SES-5", "ISSUED", utcNow().plusSeconds(180), 3, null));
            assertThat(mapper.selectMaxIssueSeqBySession("SES-5")).isEqualTo(3);
            assertThat(mapper.selectMaxIssueSeqBySession("SES-UNKNOWN")).isZero();
        }
    }
}
