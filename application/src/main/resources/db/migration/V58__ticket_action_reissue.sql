-- Allow REISSUE as a ticket action request type
ALTER TABLE public.ticket_action_request
    DROP CONSTRAINT IF EXISTS ticket_action_request_type_check;

ALTER TABLE public.ticket_action_request
    ADD CONSTRAINT ticket_action_request_type_check
        CHECK (((type)::text = ANY (ARRAY[
            ('CANCEL'::character varying)::text,
            ('VOID'::character varying)::text,
            ('REFUND'::character varying)::text,
            ('REISSUE'::character varying)::text
        ])));

-- Allow REISSUE on booking timeline status columns
ALTER TABLE public.booking_timeline
    DROP CONSTRAINT IF EXISTS booking_timeline_previous_status_check;

ALTER TABLE public.booking_timeline
    ADD CONSTRAINT booking_timeline_previous_status_check
        CHECK (((previous_status)::text = ANY (ARRAY[
            ('ALL'::character varying)::text,
            ('PROCESS'::character varying)::text,
            ('PNR'::character varying)::text,
            ('CONFIRMED'::character varying)::text,
            ('COMPLETED'::character varying)::text,
            ('CANCELLED'::character varying)::text,
            ('FAILED'::character varying)::text,
            ('BOOK'::character varying)::text,
            ('TICKETED'::character varying)::text,
            ('REJECTED'::character varying)::text,
            ('VALIDATION_PROCESS'::character varying)::text,
            ('VALIDATION_SUCCESS'::character varying)::text,
            ('VALIDATION_FAILED'::character varying)::text,
            ('VALIDATION_PRICE_CHANGED'::character varying)::text,
            ('ON_HOLD'::character varying)::text,
            ('REPRICE'::character varying)::text,
            ('VOID'::character varying)::text,
            ('TICKET_CANCELLED'::character varying)::text,
            ('REFUND'::character varying)::text,
            ('REISSUE'::character varying)::text,
            ('TICKET_ISSUED'::character varying)::text
        ])));

ALTER TABLE public.booking_timeline
    DROP CONSTRAINT IF EXISTS booking_timeline_status_check;

ALTER TABLE public.booking_timeline
    ADD CONSTRAINT booking_timeline_status_check
        CHECK (((status)::text = ANY (ARRAY[
            ('ALL'::character varying)::text,
            ('PROCESS'::character varying)::text,
            ('PNR'::character varying)::text,
            ('CONFIRMED'::character varying)::text,
            ('COMPLETED'::character varying)::text,
            ('CANCELLED'::character varying)::text,
            ('FAILED'::character varying)::text,
            ('BOOK'::character varying)::text,
            ('TICKETED'::character varying)::text,
            ('REJECTED'::character varying)::text,
            ('VALIDATION_PROCESS'::character varying)::text,
            ('VALIDATION_SUCCESS'::character varying)::text,
            ('VALIDATION_FAILED'::character varying)::text,
            ('VALIDATION_PRICE_CHANGED'::character varying)::text,
            ('ON_HOLD'::character varying)::text,
            ('REPRICE'::character varying)::text,
            ('VOID'::character varying)::text,
            ('TICKET_CANCELLED'::character varying)::text,
            ('REFUND'::character varying)::text,
            ('REISSUE'::character varying)::text,
            ('TICKET_ISSUED'::character varying)::text,
            ('SEARCH'::character varying)::text,
            ('SEARCH_FAILED'::character varying)::text,
            ('BUNDLE_VALIDATION_SUCCESS'::character varying)::text,
            ('BUNDLE_VALIDATION_FAILED'::character varying)::text,
            ('ADD_TO_CART'::character varying)::text,
            ('ADD_TO_CART_FAILED'::character varying)::text
        ])));
