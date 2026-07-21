-- Allow PROMOTION as a breaking-news target (matches BreakingNewsTarget enum)
ALTER TABLE public.breaking_news
    DROP CONSTRAINT breaking_news_target_check;

ALTER TABLE public.breaking_news
    ADD CONSTRAINT breaking_news_target_check
        CHECK (target IN ('ALL', 'USER', 'AGENCY', 'OFFER', 'PROMOTION'));
