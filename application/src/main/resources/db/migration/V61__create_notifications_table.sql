CREATE TABLE notifications (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id             BIGINT NOT NULL,
    business_id         BIGINT,
    type_code           VARCHAR(60) NOT NULL,
    category            VARCHAR(20) NOT NULL,
    priority            VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status              VARCHAR(20) NOT NULL DEFAULT 'UNREAD',
    title               VARCHAR(255) NOT NULL,
    message             TEXT NOT NULL,
    action_url          VARCHAR(500),
    action_label        VARCHAR(100),
    reference_id        VARCHAR(100),
    reference_type      VARCHAR(50),
    read_flag           BOOLEAN NOT NULL DEFAULT FALSE,
    metadata            JSONB,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    read_at             TIMESTAMP,
    archived_at         TIMESTAMP,
    expires_at          TIMESTAMP,
    created_by          BIGINT
);

CREATE INDEX idx_notifications_user_id ON notifications (user_id);
CREATE INDEX idx_notifications_user_unread ON notifications (user_id, status) WHERE status = 'UNREAD';
CREATE INDEX idx_notifications_business_id ON notifications (business_id) WHERE business_id IS NOT NULL;
CREATE INDEX idx_notifications_category ON notifications (category);
CREATE INDEX idx_notifications_reference ON notifications (reference_type, reference_id);
CREATE INDEX idx_notifications_metadata ON notifications USING GIN (metadata);