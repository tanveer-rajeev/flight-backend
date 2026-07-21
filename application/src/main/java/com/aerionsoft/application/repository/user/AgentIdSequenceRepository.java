package com.aerionsoft.application.repository.user;

import com.aerionsoft.application.entity.AgentIdSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface AgentIdSequenceRepository extends JpaRepository<AgentIdSequence, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AgentIdSequence a WHERE a.prefix = :prefix")
    Optional<AgentIdSequence> findByPrefixForUpdate(@Param("prefix") String prefix);
}

