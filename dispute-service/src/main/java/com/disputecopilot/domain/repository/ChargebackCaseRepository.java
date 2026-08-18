package com.disputecopilot.domain.repository;

import com.disputecopilot.domain.model.ChargebackCase;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargebackCaseRepository extends JpaRepository<ChargebackCase, UUID> {

    Optional<ChargebackCase> findByDisputeId(UUID disputeId);

    List<ChargebackCase> findByPendingConfirmationTrueOrderByIdDesc();
}
