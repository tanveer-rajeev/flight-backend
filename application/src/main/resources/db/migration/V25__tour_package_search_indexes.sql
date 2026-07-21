-- Supports keyword search on title, description, destination city/country at scale.

CREATE INDEX IF NOT EXISTS idx_tour_packages_status
    ON public.tour_packages (status);

CREATE INDEX IF NOT EXISTS idx_tour_packages_status_start_date
    ON public.tour_packages (status, start_date);

CREATE INDEX IF NOT EXISTS idx_tour_packages_destination_country_lower
    ON public.tour_packages (lower(destination_country));

CREATE INDEX IF NOT EXISTS idx_tour_packages_destination_city_lower
    ON public.tour_packages (lower(destination_city));

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_tour_packages_title_trgm
    ON public.tour_packages USING gin (lower(title) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_tour_packages_description_trgm
    ON public.tour_packages USING gin (lower(description) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_tour_packages_destination_city_trgm
    ON public.tour_packages USING gin (lower(destination_city) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_tour_packages_destination_country_trgm
    ON public.tour_packages USING gin (lower(destination_country) gin_trgm_ops);
