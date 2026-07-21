package com.aerionsoft.application.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "agent_id_sequence")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class AgentIdSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String prefix;

    @Builder.Default
    @Column(nullable = false)
    private Integer lastNumber = 0;
}

