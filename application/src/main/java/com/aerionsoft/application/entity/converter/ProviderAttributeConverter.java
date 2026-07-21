package com.aerionsoft.application.entity.converter;

import com.aerionsoft.application.enums.booking.Provider;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Makes Provider enum persistence tolerant to legacy/mixed DB values (e.g. "Sabre" vs "SABRE").
 *
 * Stores Provider using its stable value (Provider#getValue), but can read any of:
 * - enum name (SABRE)
 * - enum value (Sabre)
 * - any case/whitespace variant
 */
@Converter(autoApply = false)
public class ProviderAttributeConverter implements AttributeConverter<Provider, String> {

    @Override
    public String convertToDatabaseColumn(Provider attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public Provider convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        String normalized = dbData.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return Provider.getByName(normalized);
    }
}
