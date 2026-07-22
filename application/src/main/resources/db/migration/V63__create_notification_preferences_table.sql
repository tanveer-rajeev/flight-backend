CREATE TABLE notification_preferences (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    type_code           VARCHAR(60) NOT NULL,
    category            VARCHAR(20) NOT NULL,
    channel             VARCHAR(20) NOT NULL,
    enabled             BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_notification_preferences_user_type_channel
        UNIQUE (user_id, type_code, channel)
);

CREATE INDEX idx_notification_preferences_user_id ON notification_preferences (user_id);