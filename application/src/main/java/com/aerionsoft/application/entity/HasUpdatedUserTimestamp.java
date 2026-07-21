package com.aerionsoft.application.entity;

import java.time.LocalDateTime;

public interface HasUpdatedUserTimestamp {

    LocalDateTime getUpdatedAt();

    void setUpdatedAt(LocalDateTime updatedAt);

    String getUpdatedTimeOffset();

    void setUpdatedTimeOffset(String updatedTimeOffset);
}
