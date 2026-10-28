# TSP-DSN-CR-013 字段盘点与时区语义清单（生产无关盘点）

> 状态：2026-10-28，兼容阶段已实现（输出 epoch-millis，双读生效）。
> 本清单为**生产无关盘点**（源码/迁移/配置可核验项）；带 ⚠️ 的字段为**时区未核验，阻断其 ISO 切换**，
> 需授权只读取证（部署 JVM/TZ、JDBC 与数据库会话时区、列类型、可关联同一事件的脱敏时间样本）后才能解除。
> 执行边界：毫秒输出、保留双读；不扩大 VO 类型改造；不重解释/回填历史时间。

## 1. 约定
- **mapper 来源**：`MVC`=Spring ObjectMapper（框架 IovJavaTimeReadModule + IovEpochMillisWriteModule）；`Feign`=经 HttpMessageConverter 复用 Spring ObjectMapper（FW-WEB-DSN-CR-001 记录）；`Kafka(Jackson)`=翻译器注入 Spring ObjectMapper；`Kafka(hutool)`=JSONUtil 手工解析 + 显式适配器；`DB`=MyBatis/MyBatis-Plus TypeHandler + JDBC 会话时区。
- **时区语义标签**：`JVM-default`（ZoneId.systemDefault()，代码可核验，写入/读取一致则往返稳定）；`JDBC-session`（TIMESTAMP/java.util.Date 转换依赖 JDBC 会话时区）；`UTC-定义`（SPEC CR-009 "数据库 UTC"，需样本核验）；`墙上时间`（LocalDateTime 直接比较，无时区转换）；`不适用`（字符串透传/内存/非 JSON）。

## 2. MVC 出站（REST 响应 JSON，经 Spring ObjectMapper）

| 类.字段（Java 类型） | JSON key | 接口（HTTP） | 方向 | mapper | 转换/来源 | 时区语义 |
|---|---|---|---|---|---|---|
| MptTboxSummaryVo.firstLoginTime (LocalDateTime) | firstLoginTime | GET /api/mpt/tbox/v1/list | out | MVC | tb_tbox.first_login_time → TboxPo(LocalDateTime) → View → Result | ⚠️ DB 写入=JVM-default；输出 millis=atZone(Asia/Shanghai) |
| MptTboxSummaryVo.lastLoginTime (LocalDateTime) | lastLoginTime | 同上 | out | MVC | tb_tbox.last_login_time | ⚠️ 同上 |
| MptTboxSummaryVo.activateTime (LocalDateTime) | activateTime | 同上 | out | MVC | tb_tbox.activate_time | ⚠️ 同上 |
| MptTboxSummaryVo.createdAt (LocalDateTime) | createdAt | 同上 | out | MVC | tb_tbox.create_time(TIMESTAMP)→BasePo(Date)→View(LocalDateTime) | ⚠️ JDBC-session + 转换点 |
| MptTboxSummaryVo.updatedAt (LocalDateTime) | updatedAt | 同上 | out | MVC | tb_tbox.modify_time(TIMESTAMP)→BasePo(Date)→View(LocalDateTime) | ⚠️ JDBC-session + 转换点 |
| MptTboxDetailVo.bindingUpdatedAt (LocalDateTime) | bindingUpdatedAt | GET /api/mpt/tbox/v1/{sn} | out | MVC | tb_vehicle_tbox.last_event_time(DATETIME)→PO(Instant)→View(LocalDateTime) | ⚠️ 转换链含 Instant↔LocalDateTime |
| TboxTimelineItemVo.occurredAt (LocalDateTime) | activationMilestones[].occurredAt / milestones[].occurredAt | 详情/时间线 | out | MVC | TboxTimelineBuilder：Instant→LocalDateTime(JVM-default) 或 DB LocalDateTime 直读 | ⚠️ JVM-default |
| TboxAnomalyTagVo.evaluatedAt / since (LocalDateTime) | evaluatedAt / since | 列表/详情 | out | MVC | 派生（TboxAnomalyEvaluator 用 LocalDateTime.now() 计算） | 墙上时间（派生，非 DB） |
| MptTboxExportVo.*（LocalDateTime） | 导出列 | GET /api/mpt/tbox/v1/export | out | 非 JSON（Excel） | 与列表同源 | ⚠️ 与列表一致 |

## 3. MVC 入站（REST 请求参数，经 Spring ObjectMapper）

| 类.字段（Java 类型） | JSON key | 接口 | 方向 | mapper | 语义 |
|---|---|---|---|---|---|
| MptTboxQueryRequest.firstLoginFrom/To、lastLoginFrom/To、activateFrom/To、createdFrom/To (LocalDateTime) | 同名查询参数 | GET /list、/export | in | MVC（框架双读：ISO 字符串 / 数字） | 用户过滤条件，直接与 DB LocalDateTime 比较（墙上时间，无时区转换） |

## 4. Feign 出站（TSP 响应 VAGW/VMD 等，经 Spring ObjectMapper）

| 类.字段（Java 类型） | JSON key | 接口（Feign/REST） | 方向 | mapper | 时区语义 |
|---|---|---|---|---|---|
| TboxVo.activateTime/firstLoginTime/lastLoginTime (LocalDateTime) | activateTime/firstLoginTime/lastLoginTime | TspTboxInfoService GET /api/service/tbox/v1/{sn} | out | Feign=MVC mapper | ⚠️ 同 MPT（tb_tbox 三时间列） |
| DeviceAdmissionResultVo.updatedAt (Instant) | updatedAt | TspDeviceAdmissionService POST /api/service/deviceAdmission/v1/decide | out | Feign=MVC mapper | max(binding.lastEventTime, tbox.modifyTime, verification.checkedAt)；⚠️ 源各链时区未核验 |
| VehicleAccessIdentityResultVo.bindingUpdatedAt (Instant) | bindingUpdatedAt | GET /api/service/vehicleAccessIdentity/v1/byVin/{vin} | out | Feign=MVC mapper | ⚠️ tb_vehicle_tbox.last_event_time(Instant) 链 |
| VehicleTboxVo.lastEventTime / createTime (String) | lastEventTime / createTime | TspVehicleTboxService GET /service/vehicleTbox/ | out | Feign，但 **MapStruct Instant→String(toString)**，不经过 Jackson | 已是 ISO-8601 字符串（Instant.toString），非 Jackson 治理范围；⚠️ 源语义待核验 |
| VehicleCcpVo（无时间字段） | — | TspVehicleCcpService | out | — | — |

## 5. Feign 入站（TSP 消费 VMD/VAGW）

| 类.字段（Java 类型） | 来源接口 | 方向 | mapper | 语义 |
|---|---|---|---|---|
| DeviceAdmissionCheckVo.ts (Instant) | VAGW → POST decide 请求体 | in | Feign=MVC mapper（框架双读：ISO/10位秒/13位毫秒） | 绝对时刻；⚠️ 生产方单位/格式待样本核验 |
| VmdVehiclePartBindingClient.VehiclePartBindingDto.occurredAt (String) | VMD → listActiveByVin/listForBootstrap | in | Feign；**String 透传**（对账仅用 seq/bindState） | 不适用（未解析为时间类型） |

## 6. Kafka 入站

| 事件/Topic | 类.字段（Java 类型） | 消费者 | mapper | 语义 |
|---|---|---|---|---|
| vagw.device.online | VagwDeviceOnlineEvent.occurredAt (Instant) | VagwDeviceOnlineConsumer → DeviceOnlineEventTranslator | Kafka(Jackson) 框架双读 | 绝对时刻；继续接受 VAGW 数字/ISO；⚠️ 生产方格式待样本 |
| vmd.tbox-v2c-key-provisioned | TboxV2cKeyProvisionedEvent.occurredAt (Instant) | TboxV2cKeyProvisionedConsumer → Translator | Kafka(Jackson) 框架双读 | 绝对时刻；legacy 13 位毫秒（hutool 生产）已覆盖；⚠️ 生产方格式待样本 |
| vmd.vehicle-part-binding.changed | VehiclePartBindingChangedEvent.occurredAt (Instant) | VehiclePartBindingChangedConsumer | Kafka(hutool) + **WireInstantParser（CR-013 新增双读）** | 绝对时刻；ISO + 10/13 位整数；非法 fail-closed |
| ccs.sim-status-changed | （无时间字段） | VehicleSimStatusConsumer | Kafka(hutool) | — |
| vagw-tbox-event（遗留，默认关闭） | ackTime (java.util.Date，hutool getDate) | TboxEventConsumer | Kafka(hutool) | 默认禁用，不参与本期治理 |

## 7. Kafka 出站

| Topic | 类（含时间字段？） | 生产者 | 语义 |
|---|---|---|---|
| tbox-vagw-cmd | RemoteControlRequest（无 java.time 字段） | TboxCmdProducer（hutool JSONUtil.toJsonStr） | 固定单位外部协议，无 java.time，不在 Jackson 范围 |

## 8. DB 边界（写入/读取转换点，源码可核验）

| 表.列（列类型） | PO 字段（Java 类型） | 写入转换 | 读取转换 | 时区语义 |
|---|---|---|---|---|
| tb_tbox.activate_time/first_login_time/last_login_time (DATETIME) | TboxPo (LocalDateTime) | DeviceRegistrationDomainService：Instant→LocalDateTime(JVM-default) | QueryMapper → View(LocalDateTime) | ⚠️ JVM-default（写入/读取一致则往返稳定） |
| tb_tbox.create_time/modify_time (TIMESTAMP，V1 遗留) | BasePo.createTime/modifyTime (java.util.Date) | MyBatis-Plus fill + JDBC 会话时区 | JDBC 会话时区 → Date → View(LocalDateTime) | ⚠️ JDBC-session |
| tb_tbox_registration_event.occurred_at/processed_at/create_time (DATETIME) | TboxRegistrationEventPo (LocalDateTime) | TboxRegistrationEventRepositoryImpl：Instant→LocalDateTime(JVM-default) | 读取：LocalDateTime→Instant(JVM-default) | ⚠️ JVM-default |
| tb_vehicle_tbox.last_event_time / tb_vehicle_ccp.last_event_time (DATETIME) | VehicleTboxPo/VehicleCcpPo (Instant) | VehicleBindingProjectionAppService：event.occurredAt(Instant) 直写 | 直读 Instant | ⚠️ Instant→DATETIME 由 MyBatis/TypeHandler+JDBC 会话时区决定 |
| tb_vehicle_tbox.create_time / tb_vehicle_ccp.create_time (DATETIME) | BasePo (Date) | JDBC | JDBC | ⚠️ JDBC-session |
| tb_device_admission_log.checked_at (DATETIME) | DeviceAdmissionLogPo (Instant) | DeviceAdmissionConverter → 直写 Instant | 直读 Instant | ⚠️ 同 Instant→DATETIME |
| tb_vehicle_sim（无时间业务列；create/modify 同上） | VehicleSimPo (Date) | JDBC | JDBC | ⚠️ JDBC-session |

## 9. 内存/指标/头（非 JSON 治理范围）

| 位置 | 字段 | 说明 |
|---|---|---|
| DeviceOnlineConsumerState / TboxV2cKeyProvisionedConsumerState | lastPollAt/lastSuccessAt/lastDltAt (Instant) | 内存态，HealthIndicator/Metrics 输出 |
| DLT 头 | failedAt (Instant.toString) | ISO 字符串头，非 JSON |
| TboxEventDigest | occurredAt-UTC 规范化 | Instant.toString() 摘要，数字/ISO 同一时刻摘要一致（CR-010 保留） |
| TboxAnomalyEvaluator / MptTboxQueryAppService | now (LocalDateTime) | 内部计算（激活超时/离线时长），非 wire |

## 10. 待取证清单（授权只读，最小必要）
1. 部署 JVM 默认时区（各实例，含历史变更窗口）——决定 JVM-default 路径语义。
2. JDBC URL serverTimezone / 数据库会话 time_zone（连接级与 global/session）——决定 TIMESTAMP/java.util.Date 与 Instant→DATETIME 路径。
3. 相关列类型复核（DATETIME/TIMESTAMP 实际 DDL，与 §8 对照）。
4. 可关联同一事件的脱敏时间样本：同一 DEVICE_ONLINE 事件的 occurredAt（Kafka 原始值）与 tb_tbox.first_login_time 落库值、tb_tbox_registration_event.occurred_at 落库值——判定写入路径真实时区（UTC vs 上海），并关联 tb_tbox.create_time（TIMESTAMP）样本核验 JDBC-session 语义。
5. 每条证据对应字段在 §2/§4/§6/§8 的 ⚠️ 标记解除；无法取证的字段保持"时区未核验，阻断其 ISO 切换"。

## 11. 本 CR 已实现项与本清单关系
- 兼容阶段实现（epoch-millis 输出 + 框架双读 + WireInstantParser）不依赖上述取证；输出单位/双读与字段语义正交。
- ISO 切换（阶段 3）门禁：§10 取证闭环 + 消费方双读就绪签收 + 技术/业务联合评审 + CI/E2E 证据，缺一 NO-GO。
- MPT/Service VO 类型改造（LocalDateTime→Instant）未实施，登记待办：需 §10 取证确认显式转换 ZoneId 后按 CR §4.4（含 iov-tsp-api artifact 发布与消费方编译验证）另行评审。
