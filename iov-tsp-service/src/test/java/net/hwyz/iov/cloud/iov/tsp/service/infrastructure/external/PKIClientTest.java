package net.hwyz.iov.cloud.iov.tsp.service.infrastructure.external;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PKIClientTest {

    @Test
    void checkRevocation_WhenServiceUnavailable_ShouldThrowException() {
        PKIClient pkiClient = new PKIClient("http://localhost:9999", 1000);

        assertThrows(RuntimeException.class, () -> {
            pkiClient.checkRevocation("test-hsm");
        });
    }
}
