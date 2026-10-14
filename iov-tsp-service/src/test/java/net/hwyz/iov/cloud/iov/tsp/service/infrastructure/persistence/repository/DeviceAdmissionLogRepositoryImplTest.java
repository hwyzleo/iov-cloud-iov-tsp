package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.repository;

import net.hwyz.iov.cloud.iov.tsp.service.domain.deviceAdmission.model.DeviceAdmission;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.metrics.DeviceAdmissionMetrics;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.converter.DeviceAdmissionConverter;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.mapper.DeviceAdmissionLogMapper;
import net.hwyz.iov.cloud.iov.tsp.service.infrastructure.persistence.po.DeviceAdmissionLogPo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 设备接入鉴权日志仓储单元测试（TSP-DSN-CR-011 §9）
 * <p>
 * 锁定审计写失败必须计数并向上抛出（fail-closed），不得静默吞掉，
 * 以保证"每次准入审计 SHALL 记录"。
 *
 * @author hwyz_leo
 */
@ExtendWith(MockitoExtension.class)
class DeviceAdmissionLogRepositoryImplTest {

    @Mock
    private DeviceAdmissionLogMapper deviceAdmissionLogMapper;

    @Mock
    private DeviceAdmissionConverter deviceAdmissionConverter;

    @Mock
    private DeviceAdmissionMetrics deviceAdmissionMetrics;

    @InjectMocks
    private DeviceAdmissionLogRepositoryImpl repository;

    private DeviceAdmission admission() {
        return DeviceAdmission.builder().build();
    }

    @Test
    void save_whenPersistFails_shouldCountAndRethrow() {
        DeviceAdmission admission = admission();
        when(deviceAdmissionConverter.toLogPo(admission)).thenReturn(new DeviceAdmissionLogPo());
        DataIntegrityViolationException failure = new DataIntegrityViolationException("vin cannot be null");
        org.mockito.Mockito.doThrow(failure).when(deviceAdmissionLogMapper).insertPo(any());

        DataIntegrityViolationException thrown =
                assertThrows(DataIntegrityViolationException.class, () -> repository.save(admission));

        assertSame(failure, thrown);
        verify(deviceAdmissionMetrics).auditPersistFailed();
    }

    @Test
    void save_whenPersistSucceeds_shouldNotCount() {
        DeviceAdmission admission = admission();
        when(deviceAdmissionConverter.toLogPo(admission)).thenReturn(new DeviceAdmissionLogPo());

        repository.save(admission);

        verify(deviceAdmissionLogMapper).insertPo(any());
        verify(deviceAdmissionMetrics, never()).auditPersistFailed();
    }
}
