package com.aerionsoft.application.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatAttachmentDTO {

    public enum Kind {
        IMAGE,
        VOICE
    }

    private String url;
    private Kind kind;
}
