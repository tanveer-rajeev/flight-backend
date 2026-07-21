-- Add view count to tour packages
ALTER TABLE tour_packages ADD COLUMN IF NOT EXISTS view_count BIGINT NOT NULL DEFAULT 0;

-- Tour package flags (Popular, Featured)
CREATE TABLE IF NOT EXISTS tour_package_flags (
    tour_package_id BIGINT NOT NULL REFERENCES tour_packages(id) ON DELETE CASCADE,
    flag            VARCHAR(50) NOT NULL,
    PRIMARY KEY (tour_package_id, flag)
);

-- Tour categories
CREATE TABLE IF NOT EXISTS tour_categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

-- Tour package ↔ category (many-to-many)
CREATE TABLE IF NOT EXISTS tour_package_categories (
    tour_package_id BIGINT NOT NULL REFERENCES tour_packages(id) ON DELETE CASCADE,
    category_id     BIGINT NOT NULL REFERENCES tour_categories(id) ON DELETE CASCADE,
    PRIMARY KEY (tour_package_id, category_id)
);

-- User tour favourites
CREATE TABLE IF NOT EXISTS tour_favorites (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tour_package_id BIGINT NOT NULL REFERENCES tour_packages(id) ON DELETE CASCADE,
    created_at      TIMESTAMP,
    CONSTRAINT uq_tour_favorites UNIQUE (user_id, tour_package_id)
);
