-- Remove duplicate segment_airport rows (keep lowest id per segment_id + airport_type)
DELETE FROM segment_airport sa1
USING segment_airport sa2
WHERE sa1.segment_id = sa2.segment_id
  AND sa1.airport_type = sa2.airport_type
  AND sa1.id > sa2.id;

-- Remove duplicate segment_airline rows (keep lowest id per segment_id)
DELETE FROM segment_airline sa1
USING segment_airline sa2
WHERE sa1.segment_id = sa2.segment_id
  AND sa1.id > sa2.id;

ALTER TABLE segment_airport
    ADD CONSTRAINT uq_segment_airport_segment_type UNIQUE (segment_id, airport_type);

ALTER TABLE segment_airline
    ADD CONSTRAINT uq_segment_airline_segment UNIQUE (segment_id);
