UPDATE group_tickets SET ticket_type = 'A2A' WHERE UPPER(ticket_type) = 'A2I';
UPDATE booking SET group_ticket_type = 'A2A' WHERE UPPER(group_ticket_type) = 'A2I';
