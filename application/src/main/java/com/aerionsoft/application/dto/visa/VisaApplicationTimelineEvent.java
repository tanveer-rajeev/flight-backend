package com.aerionsoft.application.dto.visa;

import com.aerionsoft.application.enums.tour.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VisaApplicationTimelineEvent {
    private String eventType;
    private String title;
    private String description;
    private ApplicationStatus status;
    private String actorName;
    private LocalDateTime occurredAt;
}
