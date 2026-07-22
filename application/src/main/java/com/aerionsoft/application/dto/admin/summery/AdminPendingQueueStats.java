package com.aerionsoft.application.dto.admin.summery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminPendingQueueStats {

    private Long pendingApproval;

    public static AdminPendingQueueStats of(long count) {
        return AdminPendingQueueStats.builder().pendingApproval(count).build();
    }
}

