package com.aerionsoft.application.dto.chat;

import jakarta.validation.constraints.NotNull;
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
public class ChatWsSendRequest {

    @NotNull
    private Long conversationId;

    @Size(max = 4000)
    private String body;

    @Size(max = 5)
    private List<String> attachments;

    /** Admin only — internal note (hidden from user). */
    private Boolean isInternal;
}
