package com.aerionsoft.application.dto.admin.summery;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ActiveUsersResponse {
    private Long totalActiveUsers;
    private Long activeRegularUsers;
    private Long activeAgencyUsers;
    private Long activeAdminUsers;
    private List<ActiveUserDto> activeUsers;
}

