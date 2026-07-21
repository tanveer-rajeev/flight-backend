ALTER TABLE tour_packages ADD COLUMN IF NOT EXISTS search_count bigint NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_tour_packages_status_search_count
    ON public.tour_packages (status, search_count DESC);
