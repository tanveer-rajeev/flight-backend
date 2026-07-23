CREATE TABLE notification_templates (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    type_code                VARCHAR(60) NOT NULL,
    locale                   VARCHAR(10) NOT NULL DEFAULT 'en',
    title_template           VARCHAR(255) NOT NULL,
    message_template         TEXT NOT NULL,
    default_priority         VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    default_reference_type   VARCHAR(50),
    created_at               TIMESTAMP NOT NULL DEFAULT now(),
    updated_at               TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT uq_notification_templates_type_locale UNIQUE (type_code, locale)
);