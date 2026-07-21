package com.aerionsoft.application.dto.chat;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendChatMessageRequest {

    /** Optional when {@link #attachments} is non-empty. */
    @Size(max = 4000, message = "Message must be at most 4000 characters")
    private String body;

    /**
     * Public URLs from upload endpoints (max 5). Agency or admin.
     * Allowed: images ({@code .jpg/.jpeg/.png}) via {@code POST /api/files/upload/image},
     * voice ({@code .webm/.ogg/.mp3/.m4a/.wav/.aac}, max 60s) via {@code POST /api/files/upload/audio}.
     * Either body or at least one attachment is required.
     */
    @Size(max = 5, message = "At most 5 attachments allowed")
    private List<@Size(max = 2048) String> attachments;

    /**
     * Admin only. Internal notes are never shown to the client user
     * and are not pushed on the user WebSocket queue.
     */
    private Boolean isInternal;
}
