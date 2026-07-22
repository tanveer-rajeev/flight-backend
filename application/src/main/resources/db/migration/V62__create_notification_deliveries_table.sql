CREATE TABLE notification_deliveries (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    notification_id     BIGINT NOT NULL,
    channel             VARCHAR(20) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    recipient           VARCHAR(255) NOT NULL,
    sent_at             TIMESTAMP,
    error_message       TEXT,
    retry_count         INTEGER NOT NULL DEFAULT 0,
    delivered_at        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    created_time_offset VARCHAR(32),

    CONSTRAINT fk_notification_deliveries_notification
        FOREIGN KEY (notification_id) REFERENCES notifications (id)
        ON DELETE CASCADE
);

CREATE INDEX idx_notification_deliveries_notification_id ON notification_deliveries (notification_id);
CREATE INDEX idx_notification_deliveries_status_retry ON notification_deliveries (status, retry_count) WHERE status = 'FAILED';