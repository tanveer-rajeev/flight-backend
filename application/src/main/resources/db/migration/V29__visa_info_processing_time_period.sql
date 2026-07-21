-- processing_time stored as plain text (e.g. "P1M", "15 days").
ALTER TABLE visa_info
    ALTER COLUMN processing_time TYPE varchar(255)
    USING NULL;
