package com.aerionsoft.application.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Session metadata for a live chat thread (participants, timing, volume).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationStatsDTO {

    /** When an admin first joined (claim or admin-started chat). */
    private LocalDateTime claimedAt;

    /** First public admin reply timestamp (may differ from claim if admin only left internal notes first). */
    private LocalDateTime firstAdminReplyAt;

    /** Seconds from {@code createdAt} until {@code closedAt}, or until now if still open/active. */
    private Long durationSeconds;

    /** Seconds the user waited before an admin joined; for OPEN threads this grows until claim. */
    private Long waitTimeSeconds;

    /** Seconds from first admin join until close, or until now if still active. */
    private Long activeDurationSeconds;

    /** Public messages visible to both sides. */
    private long totalMessageCount;

    private long userMessageCount;

    /** Public admin replies (excludes internal notes). */
    private long adminMessageCount;

    /** Admin-only internal notes. */
    private long internalNoteCount;

    /** Display name of whoever closed the thread. */
    private String closedByName;
}
