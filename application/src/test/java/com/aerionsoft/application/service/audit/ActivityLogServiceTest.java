package com.aerionsoft.application.service.audit;

import com.aerionsoft.application.entity.audit.ActivityLog;
import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.enums.audit.ActivityOutcome;
import com.aerionsoft.application.enums.audit.ActorType;
import com.aerionsoft.application.repository.audit.ActivityLogRepository;
import com.aerionsoft.application.util.ActorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceTest {

    @Mock
    private ActivityLogRepository activityLogRepository;

    private ActivityLogService activityLogService;

    @BeforeEach
    void setUp() {
        Executor directExecutor = Runnable::run;
        activityLogService = new ActivityLogService(activityLogRepository, directExecutor);
        ReflectionTestUtils.setField(activityLogService, "enabled", true);
        when(activityLogRepository.save(any(ActivityLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void persist_redactsSensitiveMetadata() {
        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(ActivityEventType.USER_LOGIN)
                .outcome(ActivityOutcome.SUCCESS)
                .actor(ActorContext.forUser(10L, "user@test.com"))
                .metadata(Map.of("password", "secret123", "channel", "web"))
                .build());

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());

        ActivityLog saved = captor.getValue();
        assertEquals(ActivityEventType.USER_LOGIN, saved.getEventType());
        assertEquals(ActorType.USER, saved.getActorType());
        assertEquals(10L, saved.getActorId());
        assertNotNull(saved.getMetadata());
        assertTrue(saved.getMetadata().contains("[REDACTED]"));
        assertTrue(saved.getMetadata().contains("web"));
    }

    @Test
    void persist_recordsFailedLoginForGuestActor() {
        activityLogService.log(ActivityLogService.ActivityLogEntry.builder()
                .eventType(ActivityEventType.LOGIN_FAILED)
                .outcome(ActivityOutcome.FAILURE)
                .actor(ActorContext.guest())
                .description("Invalid credentials")
                .metadata(Map.of("email", "missing@test.com", "portal", "admin"))
                .ipAddress("127.0.0.1")
                .build());

        ArgumentCaptor<ActivityLog> captor = ArgumentCaptor.forClass(ActivityLog.class);
        verify(activityLogRepository).save(captor.capture());

        ActivityLog saved = captor.getValue();
        assertEquals(ActivityOutcome.FAILURE, saved.getOutcome());
        assertEquals(ActorType.GUEST, saved.getActorType());
        assertEquals("127.0.0.1", saved.getIpAddress());
    }
}
