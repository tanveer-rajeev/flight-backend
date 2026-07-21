package com.aerionsoft.application.dto.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatUserSearchItemDTO {

    private Long userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String code;
    private boolean agency;
    private Long businessId;
    private String businessName;
    /** True when user already has an OPEN or ACTIVE live chat. */
    private boolean hasOpenChat;
    private Long openConversationId;
}
