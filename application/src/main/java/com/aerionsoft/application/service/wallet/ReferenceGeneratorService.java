package com.aerionsoft.application.service.wallet;

import com.aerionsoft.application.repository.wallet.WalletReferenceSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class ReferenceGeneratorService {

    private final WalletReferenceSequenceRepository sequenceRepository;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    public ReferenceGeneratorService(WalletReferenceSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Transactional
    public String nextReference(String prefix) {
        LocalDate today = LocalDate.now();

        int next = sequenceRepository.incrementAndGet(prefix, today);

        String datePart = today.format(DATE_FMT);
        String numberPart = String.format("%04d", next);
        return prefix + datePart + numberPart;
    }
}
