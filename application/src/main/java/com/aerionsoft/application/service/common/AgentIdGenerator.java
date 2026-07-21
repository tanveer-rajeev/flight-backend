package com.aerionsoft.application.service.common;

import com.aerionsoft.application.entity.AgentIdSequence;
import com.aerionsoft.application.repository.user.AgentIdSequenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentIdGenerator {

    private final String platformPrefix;
    private static final int START_NUMBER = 5000;

    @Autowired
    private AgentIdSequenceRepository sequenceRepository;

    public AgentIdGenerator(@Value("${platform.name:K}") String platformName) {
        // Use the first character of platform.name; default to "K" if missing
        this.platformPrefix = platformName != null && !platformName.isBlank()
                ? platformName.substring(0, 1).toUpperCase()
                : "K";
    }

    @Transactional
    public String generate(String currency, String sixDigitId) {
        String sanitizedCurrency =
                (currency == null || currency.isBlank())
                        ? "U"
                        : currency.trim().toUpperCase().substring(0, 1);

        // If sixDigitId is explicitly provided, use it directly
        if (sixDigitId != null && !sixDigitId.isBlank()) {
            return platformPrefix + sanitizedCurrency + sixDigitId;
        }

        // Otherwise, generate incremental ID from sequence
        String prefix = platformPrefix + sanitizedCurrency;
        String idPart = getNextSequentialId(prefix);

        return prefix + idPart;
    }




    private String getNextSequentialId(String prefix) {
        AgentIdSequence sequence = sequenceRepository.findByPrefixForUpdate(prefix)
                .orElseGet(() -> {
                    AgentIdSequence newSequence = AgentIdSequence.builder()
                            .prefix(prefix)
                            .lastNumber(START_NUMBER - 1) // 4999
                            .build();
                    return sequenceRepository.save(newSequence);
                });

        int nextNumber = sequence.getLastNumber() + 1;
        sequence.setLastNumber(nextNumber);
        sequenceRepository.save(sequence);

        // 4-digit formatting
        return String.format("%04d", nextNumber);
    }

}
