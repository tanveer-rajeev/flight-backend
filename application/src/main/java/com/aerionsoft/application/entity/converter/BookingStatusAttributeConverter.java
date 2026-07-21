package com.aerionsoft.application.entity.converter;

import com.aerionsoft.application.enums.booking.BookingStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Map;

/**
 * Tolerates legacy / misspelled booking status values in the database.
 */
@Converter(autoApply = false)
public class BookingStatusAttributeConverter implements AttributeConverter<BookingStatus, String> {

    private static final Map<String, BookingStatus> LEGACY_ALIASES = Map.of(
            "CONFRIMED", BookingStatus.CONFIRMED
    );

    @Override
    public String convertToDatabaseColumn(BookingStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public BookingStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String normalized = dbData.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        String upper = normalized.toUpperCase();
        BookingStatus alias = LEGACY_ALIASES.get(upper);
        if (alias != null) {
            return alias;
        }
        return BookingStatus.valueOf(upper);
    }
}
