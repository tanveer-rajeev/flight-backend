package com.aerionsoft.application.dto.chat;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatWsTypingRequest {

    @NotNull
    private Long conversationId;

    private boolean typing;
}
