package net.hwyz.iov.cloud.iov.tsp.service.adapter.mq.consumer.support;

import net.hwyz.iov.cloud.framework.kafka.support.KafkaAdminOperations;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.config.TspDeviceOnlineProperties;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.TopicDescription;
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

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceOnlineConsumerHealthIndicatorTest {

    @Mock
    private ObjectProvider<Admin> adminProvider;
    @Mock
    private KafkaAdminOperations kafkaAdminOperations;
    @Mock
    private Admin admin;
    @Mock
    private TopicDescription topicDescription;

    private TspDeviceOnlineProperties properties;
    private DeviceOnlineConsumerState state;
    private DeviceOnlineConsumerHealthIndicator indicator;

    @BeforeEach
    void setUp() {
        properties = new TspDeviceOnlineProperties();
        state = new DeviceOnlineConsumerState();
        when(adminProvider.getIfAvailable()).thenReturn(admin);
        indicator = new DeviceOnlineConsumerHealthIndicator(properties, state, adminProvider, kafkaAdminOperations);
    }

    @Test
    void health_Disabled_ShouldBeOutOfService() {
        properties.setEnabled(false);
        assertEquals(Status.OUT_OF_SERVICE, indicator.health().getStatus());
    }

    @Test
    void health_NotStarted_ShouldBeDown() {
        assertEquals(Status.DOWN, indicator.health().getStatus());
    }

    @Test
    void health_TopicReady_ShouldBeUp() throws Exception {
        state.setStarted(true);
        when(topicDescription.partitions()).thenReturn(Collections.emptyList());
        when(kafkaAdminOperations.describeTopics(any(), anyCollection(), any()))
                .thenReturn(java.util.Map.of("vagw.device.online", topicDescription));

        assertEquals(Status.UP, indicator.health().getStatus());
    }

    @Test
    void health_TopicUnknown_ShouldBeDown() throws Exception {
        state.setStarted(true);
        when(kafkaAdminOperations.describeTopics(any(), anyCollection(), any()))
                .thenThrow(new UnknownTopicOrPartitionException("missing"));

        assertEquals(Status.DOWN, indicator.health().getStatus());
    }
}
