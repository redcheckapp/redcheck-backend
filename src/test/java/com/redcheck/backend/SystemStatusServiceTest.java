package com.redcheck.backend;

import com.redcheck.backend.dto.response.SystemStatusResponseDTO;
import com.redcheck.backend.service.SystemStatusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Unitary Tests - SystemStatusService")
public class SystemStatusServiceTest {

    @Test
    @DisplayName("An unreachable AI engine should report as down, not throw")
    @SuppressWarnings("unchecked")
    void getStatus_WhenEngineUnreachable_ShouldReportDown() {
        // GIVEN: port 1 refuses connections immediately.
        ObjectProvider<BuildProperties> provider = mock(ObjectProvider.class);
        Properties props = new Properties();
        props.setProperty("version", "1.2.3");
        when(provider.getIfAvailable()).thenReturn(new BuildProperties(props));
        SystemStatusService service = new SystemStatusService(provider, "http://localhost:1", "abc1234");

        // WHEN
        SystemStatusResponseDTO status = service.getStatus();

        // THEN
        assertFalse(status.aiEngineReachable());
        assertNull(status.aiEngineLatencyMs());
        assertEquals("1.2.3", status.version());
        assertEquals("abc1234", status.gitSha());
        assertTrue(status.memoryMaxMb() > 0);
        assertNotNull(status.startedAt());
    }

    @Test
    @DisplayName("Missing build info (e.g. run from an IDE) should leave version/buildTime empty")
    @SuppressWarnings("unchecked")
    void getStatus_WithoutBuildInfo_ShouldNotFail() {
        ObjectProvider<BuildProperties> provider = mock(ObjectProvider.class);
        SystemStatusService service = new SystemStatusService(provider, "http://localhost:1", "unknown");

        SystemStatusResponseDTO status = service.getStatus();

        assertNull(status.version());
        assertNull(status.buildTime());
    }
}
