package com.aerionsoft.application.entity.wallet;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "wallet_reference_sequence", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"prefix", "ref_date"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletReferenceSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 8)
    private String prefix;

    @Column(name = "ref_date", nullable = false)
    private LocalDate refDate;

    @Column(name = "last_number", nullable = false)
    private Integer lastNumber;
}

