package com.aerionsoft.application.service.audit;

import com.aerionsoft.application.enums.audit.ActivityEventType;
import com.aerionsoft.application.enums.audit.ActivityOutcome;
import com.aerionsoft.application.enums.booking.BookingStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ActivityBookingAuditSupportAdminRefundTest {

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ActivityBookingAuditSupport support;

    @Test
    void logAdminRefund_writesBookingRefundedWithAppendOnlyMetadata() throws Exception {
        Map<String, Object> refundMeta = new LinkedHashMap<>();
        refundMeta.put("channel", "ADMIN_REFUND");
        refundMeta.put("appendOnly", true);
        refundMeta.put("refundType", "FULL");
        refundMeta.put("refundedAmount", new BigDecimal("1200.00"));
        refundMeta.put("supplierRefundCost", new BigDecimal("200.00"));
        refundMeta.put("supplierPayableReversed", new BigDecimal("700.00"));

        support.logAdminRefund(42L, "ABC123", BookingStatus.TICKETED, "Customer cancelled", refundMeta);

        ArgumentCaptor<ActivityLogService.ActivityLogEntry> captor =
                ArgumentCaptor.forClass(ActivityLogService.ActivityLogEntry.class);
        verify(activityLogService).log(captor.capture());

        ActivityLogService.ActivityLogEntry entry = captor.getValue();
        assertThat(field(entry, "eventType")).isEqualTo(ActivityEventType.BOOKING_REFUNDED);
        assertThat(field(entry, "outcome")).isEqualTo(ActivityOutcome.SUCCESS);
        assertThat(field(entry, "resourceType")).isEqualTo("BOOKING");
        assertThat(field(entry, "resourceId")).isEqualTo("42");
        assertThat(field(entry, "description")).isEqualTo("Admin booking refund processed");

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) field(entry, "metadata");
        assertThat(metadata)
                .containsEntry("channel", "ADMIN_REFUND")
                .containsEntry("appendOnly", true)
                .containsEntry("oldStatus", "TICKETED")
                .containsEntry("newStatus", "REFUND")
                .containsEntry("refundType", "FULL")
                .containsEntry("reason", "Customer cancelled");
    }

    private static Object field(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }
}
