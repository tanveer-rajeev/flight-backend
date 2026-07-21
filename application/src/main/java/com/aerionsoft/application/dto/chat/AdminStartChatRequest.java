package com.aerionsoft.application.dto.chat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStartChatRequest {

    /** Target client user id (`users.id`). */
    @NotNull(message = "userId is required")
    private Long userId;

    @Size(max = 255, message = "Subject must be at most 255 characters")
    private String subject;

    /** Optional first message from the admin (public, not internal). */
    @Size(max = 4000, message = "Message must be at most 4000 characters")
    private String initialMessage;
}
