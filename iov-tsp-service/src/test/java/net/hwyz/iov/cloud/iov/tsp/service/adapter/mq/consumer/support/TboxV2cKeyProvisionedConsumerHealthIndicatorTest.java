package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import net.hwyz.iov.cloud.framework.kafka.support.KafkaAdminOperations;
import net.hwyz.iov.cloud.iov.tsp.service.domain.repository.TboxRegistrationEventRepository;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspTboxV2cKeyProvisionedProperties;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * V2C 激活 readiness 健康检查测试（TSP-DSN-CR-012 §9）
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TboxV2cKeyProvisionedConsumerHealthIndicatorTest {

    private static final String TOPIC = "vmd.tbox-v2c-key-provisioned";
    private static final String GROUP = "iov-tsp-tbox-v2c-key-provisioned";

    @Mock
    private TboxRegistrationEventRepository repository;
    @Mock
    private KafkaAdminOperations kafkaAdminOperations;
    @Mock
    private Admin admin;
    @Mock
    private TopicDescription topicDescription;
    @Mock
    private ConsumerGroupDescription groupDescription;
    @Mock
    private DescribeConsumerGroupsResult describeConsumerGroupsResult;

    private TspTboxV2cKeyProvisionedProperties properties;
    private TboxV2cKeyProvisionedConsumerState state;
    private TboxV2cKeyProvisionedConsumerHealthIndicator indicator;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        properties = new TspTboxV2cKeyProvisionedProperties();
        properties.setEnabled(true);
        properties.setActiveProducerRelease("VMD-CR-062");
        state = new TboxV2cKeyProvisionedConsumerState();
        state.setStarted(true);
        state.setSubscribed(true);

        ObjectProvider<Admin> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(admin);
        when(repository.findByEventId(anyString())).thenReturn(Optional.empty());
        when(kafkaAdminOperations.describeTopics(any(), any(), any()))
                .thenReturn(Map.of(TOPIC, topicDescription));
        when(topicDescription.partitions()).thenReturn(List.of());
        when(groupDescription.state()).thenReturn(ConsumerGroupState.STABLE);
        when(admin.describeConsumerGroups(anyCollection())).thenReturn(describeConsumerGroupsResult);
        when(describeConsumerGroupsResult.all())
                .thenReturn(KafkaFuture.completedFuture(Map.of(GROUP, groupDescription)));

        indicator = new TboxV2cKeyProvisionedConsumerHealthIndicator(
                properties, state, repository, provider, kafkaAdminOperations);
    }

    @Test
    void health_WhenGateDisabled_ShouldBeOutOfService() {
        properties.setEnabled(false);
        assertEquals(Status.OUT_OF_SERVICE, indicator.health().getStatus());
    }

    @Test
    void health_WhenContractVersionUnsupported_ShouldBeDown() {
        properties.setSupportedContractVersions(Set.of("2"));
        assertEquals(Status.DOWN, indicator.health().getStatus());
    }

    @Test
    void health_WhenDltDisabled_ShouldBeDown() {
        properties.getDlt().setEnabled(false);
        assertEquals(Status.DOWN, indicator.health().getStatus());
    }

    @Test
    void health_WhenNotSubscribed_ShouldBeDown() {
        state.setSubscribed(false);
        assertEquals(Status.DOWN, indicator.health().getStatus());
    }

    @Test
    void health_WhenInboxUnavailable_ShouldBeDown() {
        when(repository.findByEventId(anyString())).thenThrow(new IllegalStateException("db down"));
        assertEquals(Status.DOWN, indicator.health().getStatus());
    }

    @Test
    void health_WhenTopicMissing_ShouldBeDown() throws Exception {
        when(kafkaAdminOperations.describeTopics(any(), any(), any()))
                .thenThrow(new UnknownTopicOrPartitionException("missing"));
        assertEquals(Status.DOWN, indicator.health().getStatus());
    }

    @Test
    void health_WhenConsumerGroupEmpty_ShouldBeDown() {
        when(groupDescription.state()).thenReturn(ConsumerGroupState.EMPTY);
        assertEquals(Status.DOWN, indicator.health().getStatus());
    }

    @Test
    void health_WhenProducerReleaseGateNotSatisfied_ShouldBeDown() {
        properties.setActiveProducerRelease(null);
        assertEquals(Status.DOWN, indicator.health().getStatus());
    }

    @Test
    void health_WhenAllReady_ShouldBeUp() {
        Health health = indicator.health();
        assertEquals(Status.UP, health.getStatus());
        assertEquals(ConsumerGroupState.STABLE.name(), health.getDetails().get("consumerGroupState"));
    }
}
