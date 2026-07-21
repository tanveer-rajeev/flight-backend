package com.aerionsoft.application.entity;

import java.time.LocalDateTime;

public interface HasCreatedUserTimestamp {

    LocalDateTime getCreatedAt();

    void setCreatedAt(LocalDateTime createdAt);

    String getCreatedTimeOffset();

    void setCreatedTimeOffset(String createdTimeOffset);
}
