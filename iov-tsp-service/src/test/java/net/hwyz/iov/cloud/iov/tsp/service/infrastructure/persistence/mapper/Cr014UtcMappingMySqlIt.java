package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper;

import net.hwyz.iov.cloud.iov.tsp.service.domain.model.enums.DeviceStatus;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.TboxPo;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.VehicleTboxPo;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TSP-DSN-CR-014：真实 MySQL 集成测试（隔离 schema db_tsp_cr014，docker 容器）。
 * <p>
 * 验证（RD-TIME-002/005/006）：
 * <ol>
 *   <li>Instant 字段（last_event_time / checked_at）经 UtcInstantTypeHandler 双向映射，
 *       DB 中存储 UTC 墙钟（不随 JVM 时区变化）；</li>
 *   <li>普通审计单属主：create_time/modify_time 由 DB DEFAULT/ON UPDATE 唯一生成；</li>
 *   <li>recordLoginById set-once/max 语义保持（first_login_time 不变、last_login_time 取 max）；</li>
 *   <li>跨 JVM 交叉读：UTC JVM 写入的行在 Asia/Shanghai JVM 中读回同一绝对时刻。</li>
 * </ol>
 * 运行（本机 docker 容器 tsp-cr014-mysql，端口 13306）：
 * <pre>
 *   mvn -o test -pl iov-tsp-service -Dtest=Cr014UtcMappingMySqlIt -Duser.timezone=UTC \
 *       -Dcr014.cross.jvm=utc-write
 *   mvn -o test -pl iov-tsp-service -Dtest=Cr014UtcMappingMySqlIt -Duser.timezone=Asia/Shanghai \
 *       -Dcr014.cross.jvm=sh-read
 * </pre>
 * 类名以 It 结尾，surefire 默认不执行；DB 不可用时用例跳过（本地验证库，非生产）。
 *
 * @author hwyz_leo
 * @since 2026-10-31
 */
class Cr014UtcMappingMySqlIt {

    private static final String DB_URL_BASE = "jdbc:mysql://%s:%s/db_tsp_cr014?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=utf8";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "cr014_root_pw";

    /** 跨 JVM 共享行固定 bindingId（UTC 写 → 上海读） */
    private static final long CROSS_JVM_BINDING_ID = 900000000L;
    private static final Instant CROSS_JVM_INSTANT = Instant.parse("2026-10-10T08:00:00Z");
    private static final String CROSS_JVM_UTC_WALL = "2026-10-10 08:00:00";

    private static SqlSessionFactory sqlSessionFactory;
    private static boolean dbAvailable;

    @BeforeAll
    static void setup() {
        String host = System.getProperty("cr014.db.host", "localhost");
        String port = System.getProperty("cr014.db.port", "13306");
        try {
            PooledDataSource dataSource = new PooledDataSource();
            dataSource.setDriver("com.mysql.cj.jdbc.Driver");
            dataSource.setUrl(String.format(DB_URL_BASE, host, port));
            dataSource.setUsername(DB_USER);
            dataSource.setPassword(DB_PASSWORD);
            Configuration configuration = new Configuration();
            configuration.setEnvironment(new Environment("cr014-it", new JdbcTransactionFactory(), dataSource));
            for (String xml : new String[]{
                    "/mappers/VehicleTboxMapper.xml",
                    "/mappers/VehicleCcpMapper.xml",
                    "/mappers/TboxMapper.xml"}) {
                try (InputStream in = Cr014UtcMappingMySqlIt.class.getResourceAsStream(xml)) {
                    if (in == null) {
                        throw new IllegalStateException("未找到 " + xml);
                    }
                    new XMLMapperBuilder(in, configuration, xml, configuration.getSqlFragments()).parse();
                }
            }
            // XML namespace 解析会自动注册对应 mapper 接口；
            // TboxRegistrationEventMapper 为注解 SQL（无 XML），需显式注册。
            configuration.addMapper(TboxRegistrationEventMapper.class);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
            dbAvailable = true;
            System.out.println("[CR014-IT] JVM 时区=" + TimeZone.getDefault().getID()
                    + "，DB=" + host + ":" + port + "/db_tsp_cr014");
        } catch (Exception e) {
            dbAvailable = false;
            System.out.println("[CR014-IT] 数据库不可用，跳过： " + e.getMessage());
        }
    }

    private void requireDb() {
        Assumptions.assumeTrue(dbAvailable, "本地隔离 MySQL 不可用，跳过真实 DB 断言");
    }

    // ---------- 1. Instant ↔ UTC DATETIME 双向映射 ----------

    @Test
    void lastEventTime_RoundTrip_ShouldPreserveInstantAcrossJvmTimeZone() {
        requireDb();
        Instant event = Instant.parse("2026-10-10T08:00:00Z");
        long unique = System.nanoTime() % 100_000_000L;
        long bindingId = 1_000_000L + unique;
        String vin = "ITVIN-RT-" + unique;

        try (SqlSession session = sqlSessionFactory.openSession()) {
            VehicleTboxMapper mapper = session.getMapper(VehicleTboxMapper.class);
            VehicleTboxPo po = VehicleTboxPo.builder()
                    .vin(vin).sn("ITSN-RT-" + unique).bindingId(bindingId)
                    .deviceCategory("TBOX").bindState("ACTIVE")
                    .bindingVersion(1L).lastEventTime(event).source("VMD")
                    .build();
            assertTrue(mapper.upsertByBindingId(po) >= 1, "upsert 应插入或更新共享行");
            session.commit();

            VehicleTboxPo read = mapper.selectByBindingId(bindingId);
            assertNotNull(read, "upsert 后应可读回");
            assertEquals(event, read.getLastEventTime(),
                    "读回必须与写入为同一绝对时刻（当前 JVM=" + TimeZone.getDefault().getID() + "）");
        }
    }

    @Test
    void lastEventTime_DbShouldStoreUtcWallClock() throws Exception {
        requireDb();
        long unique = System.nanoTime() % 100_000_000L;
        long bindingId = 2_000_000L + unique;
        String vin = "ITVIN-WALL-" + unique;
        Instant event = Instant.parse("2026-10-10T08:00:00Z");

        try (SqlSession session = sqlSessionFactory.openSession()) {
            VehicleTboxMapper mapper = session.getMapper(VehicleTboxMapper.class);
            mapper.upsertByBindingId(VehicleTboxPo.builder()
                    .vin(vin).sn("ITSN-WALL-" + unique).bindingId(bindingId)
                    .deviceCategory("TBOX").bindState("ACTIVE")
                    .bindingVersion(1L).lastEventTime(event).source("VMD")
                    .build());
            session.commit();
        }
        try (Connection conn = DriverManager.getConnection(
                String.format(DB_URL_BASE, "localhost", "13306"), DB_USER, DB_PASSWORD);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT last_event_time FROM tb_vehicle_tbox WHERE binding_id = " + bindingId)) {
            assertTrue(rs.next());
            assertEquals(CROSS_JVM_UTC_WALL, rs.getString(1),
                    "DB 必须存储 UTC 墙钟（不随 JVM 时区变化），实际=" + rs.getString(1));
        }
    }

    @Test
    void auditTimes_ShouldBeGeneratedByDbOnInsert() {
        requireDb();
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TboxMapper mapper = session.getMapper(TboxMapper.class);
            TboxPo po = TboxPo.builder().sn("ITSN-AUDIT-" + System.nanoTime() % 100_000)
                    .hsm("HSM-IT-1-" + System.nanoTime() % 100_000).iccid1("46000000000000000000").build();
            mapper.insertPo(po);
            session.commit();

            TboxPo read = mapper.selectPoById(po.getId());
            assertNotNull(read.getCreateTime(), "create_time 应由 DB DEFAULT 生成");
            assertNotNull(read.getModifyTime(), "modify_time 应由 DB DEFAULT 生成");
        }
    }

    @Test
    void auditModifyTime_ShouldBeUpdatedByOnUpdate() throws Exception {
        requireDb();
        long ts = System.nanoTime() % 100_000;
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TboxMapper mapper = session.getMapper(TboxMapper.class);
            TboxPo po = TboxPo.builder().sn("ITSN-UPD-" + ts).hsm("HSM-IT-2-" + ts)
                    .iccid1("46000000000000000001").build();
            mapper.insertPo(po);
            session.commit();

            java.util.Date before;
            try (Connection conn = DriverManager.getConnection(
                    String.format(DB_URL_BASE, "localhost", "13306"), DB_USER, DB_PASSWORD);
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT modify_time FROM tb_tbox WHERE id = " + po.getId())) {
                rs.next();
                before = rs.getTimestamp(1);
            }

            Thread.sleep(1100); // TIMESTAMP 秒级，等待跨秒
            po.setDescription("updated");
            mapper.updatePo(po);
            session.commit();

            try (Connection conn = DriverManager.getConnection(
                    String.format(DB_URL_BASE, "localhost", "13306"), DB_USER, DB_PASSWORD);
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT modify_time FROM tb_tbox WHERE id = " + po.getId())) {
                rs.next();
                java.util.Date after = rs.getTimestamp(1);
                assertTrue(after.after(before),
                        "modify_time 应随行更新（ON UPDATE），before=" + before + " after=" + after);
            }
        }
    }

    // ---------- 3. VIAM 投影写语义保持（IOV-TSP-DSN-CR-016 替代 recordLoginById） ----------

    @Test
    void applyViamLoginFact_LastMax_FirstAndStatusNotWritten() {
        requireDb();
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TboxMapper mapper = session.getMapper(TboxMapper.class);
            TboxPo po = TboxPo.builder().sn("ITSN-LOGIN-" + System.nanoTime() % 100_000)
                    .hsm("HSM-IT-3-" + System.nanoTime() % 100_000).iccid1("46000000000000000002")
                    .build();
            mapper.insertPo(po);
            session.commit();

            LocalDateTime first = LocalDateTime.of(2026, 10, 10, 8, 0, 0);
            LocalDateTime later = LocalDateTime.of(2026, 10, 11, 8, 0, 0);
            LocalDateTime earliest = LocalDateTime.of(2026, 10, 9, 8, 0, 0);

            // 第一次登录事实：last = first；first_login_time 不写（由快照承载）；device_status 不推进（投影链唯一写者）
            assertEquals(1, mapper.applyViamLoginFact(po.getId(), first,
                    LocalDateTime.of(2026, 10, 10, 9, 0, 0), "PARTIAL"));
            session.commit();
            TboxPo read1 = mapper.selectPoById(po.getId());
            assertNull(read1.getFirstLoginTime(), "first_login_time 不由登录事实写入（快照承载）");
            assertEquals(first, read1.getLastLoginTime(), "last_login_time 首次写入");
            assertNull(read1.getDeviceStatus(), "device_status 不由登录事实写入（投影链唯一写者）");
            assertEquals("PARTIAL", read1.getViamSyncStatus());

            // 第二次：较晚时间，last 取 max
            mapper.applyViamLoginFact(po.getId(), later,
                    LocalDateTime.of(2026, 10, 11, 9, 0, 0), "PARTIAL");
            session.commit();
            TboxPo read2 = mapper.selectPoById(po.getId());
            assertNull(read2.getFirstLoginTime());
            assertEquals(later, read2.getLastLoginTime(), "last_login_time 取 max");

            // 第三次：更早时间，last 保持 max、不倒退
            mapper.applyViamLoginFact(po.getId(), earliest,
                    LocalDateTime.of(2026, 10, 11, 10, 0, 0), "PARTIAL");
            session.commit();
            TboxPo read3 = mapper.selectPoById(po.getId());
            assertEquals(later, read3.getLastLoginTime(), "last_login_time 不得倒退（max）");
        }
    }

    @Test
    void applyViamSnapshot_FirstFromSnapshot_LastMax() {
        requireDb();
        try (SqlSession session = sqlSessionFactory.openSession()) {
            TboxMapper mapper = session.getMapper(TboxMapper.class);
            TboxPo po = TboxPo.builder().sn("ITSN-SNAP-" + System.nanoTime() % 100_000)
                    .hsm("HSM-IT-4-" + System.nanoTime() % 100_000).iccid1("46000000000000000002")
                    .build();
            mapper.insertPo(po);
            session.commit();

            LocalDateTime first = LocalDateTime.of(2026, 10, 10, 8, 0, 0);
            LocalDateTime last = LocalDateTime.of(2026, 10, 12, 8, 0, 0);
            mapper.applyViamSnapshot(po.getId(), DeviceStatus.ACTIVE.getCode(), first, last,
                    null, LocalDateTime.of(2026, 10, 12, 9, 0, 0), "SYNCED");
            session.commit();
            TboxPo read = mapper.selectPoById(po.getId());
            assertEquals(DeviceStatus.ACTIVE.getCode(), read.getDeviceStatus(), "快照 state 映射写 device_status");
            assertEquals(first, read.getFirstLoginTime(), "first_login_time 以 VIAM 快照为准");
            assertEquals(last, read.getLastLoginTime());
            assertEquals("SYNCED", read.getViamSyncStatus());
        }
    }

    // ---------- 4. 跨 JVM 交叉读（UTC 写 → Asia/Shanghai 读同一行） ----------

    @Test
    void crossJvm_UtcWriter_PrepareSharedRow() {
        String role = System.getProperty("cr014.cross.jvm", "");
        Assumptions.assumeTrue("utc-write".equals(role), "仅 UTC JVM 执行共享行写入");
        requireDb();
        try (SqlSession session = sqlSessionFactory.openSession()) {
            VehicleTboxMapper mapper = session.getMapper(VehicleTboxMapper.class);
            mapper.upsertByBindingId(VehicleTboxPo.builder()
                    .vin("ITVIN-CROSS").sn("ITSN-CROSS").bindingId(CROSS_JVM_BINDING_ID)
                    .deviceCategory("TBOX").bindState("ACTIVE")
                    .bindingVersion(1L).lastEventTime(CROSS_JVM_INSTANT).source("VMD")
                    .build());
            session.commit();
        }
        System.out.println("[CR014-IT] UTC JVM 已写入共享行 binding_id=" + CROSS_JVM_BINDING_ID
                + " instant=" + CROSS_JVM_INSTANT);
    }

    @Test
    void crossJvm_ShanghaiReader_ShouldReadSameInstant() {
        String role = System.getProperty("cr014.cross.jvm", "");
        Assumptions.assumeTrue("sh-read".equals(role), "仅 Asia/Shanghai JVM 执行共享行读取");
        requireDb();
        try (SqlSession session = sqlSessionFactory.openSession()) {
            VehicleTboxMapper mapper = session.getMapper(VehicleTboxMapper.class);
            VehicleTboxPo read = mapper.selectByBindingId(CROSS_JVM_BINDING_ID);
            assertNotNull(read, "UTC JVM 应先写入共享行（先跑 utc-write）");
            assertEquals(CROSS_JVM_INSTANT, read.getLastEventTime(),
                    "Asia/Shanghai JVM 读回必须与 UTC JVM 写入为同一绝对时刻（JVM="
                            + TimeZone.getDefault().getID() + "）");
        }
    }
}
