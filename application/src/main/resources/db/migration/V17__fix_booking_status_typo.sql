-- Fix legacy typo in booking status values (blocks Hibernate enum mapping / global search).
UPDATE booking
SET status = 'CONFIRMED'
WHERE status = 'CONFRIMED';

UPDATE booking_timeline
SET status = 'CONFIRMED'
WHERE status = 'CONFRIMED';

UPDATE booking_timeline
SET previous_status = 'CONFIRMED'
WHERE previous_status = 'CONFRIMED';
