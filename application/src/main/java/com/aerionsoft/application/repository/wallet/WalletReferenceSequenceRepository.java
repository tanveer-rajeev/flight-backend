package com.aerionsoft.application.repository.wallet;

import com.aerionsoft.application.entity.wallet.WalletReferenceSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface WalletReferenceSequenceRepository extends JpaRepository<WalletReferenceSequence, Long> {

    /**
     * Atomically increments the sequence row for (prefix, ref_date), creating it if missing.
     *
     * <p>PostgreSQL-only implementation using INSERT .. ON CONFLICT .. DO UPDATE .. RETURNING.
     * Requires a unique constraint on (prefix, ref_date).
     *
     * <p>IMPORTANT: Do NOT annotate with {@code @Modifying}. This statement returns a row via
     * {@code RETURNING}, and marking it as modifying would make Spring execute it as an update
     * count operation (executeUpdate), causing "A result was returned when none was expected".
     *
     * @return the updated last_number
     */
    @Query(value = "INSERT INTO wallet_reference_sequence(prefix, ref_date, last_number) " +
            "VALUES (:prefix, :refDate, 1) " +
            "ON CONFLICT (prefix, ref_date) " +
            "DO UPDATE SET last_number = wallet_reference_sequence.last_number + 1 " +
            "RETURNING last_number",
            nativeQuery = true)
    int incrementAndGet(@Param("prefix") String prefix, @Param("refDate") LocalDate refDate);
}
