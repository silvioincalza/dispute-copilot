package com.disputecopilot.domain.repository;

import com.disputecopilot.domain.model.Dispute;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    @Override
    @EntityGraph(attributePaths = "chargebackCase")
    List<Dispute> findAll();

    @Override
    @EntityGraph(attributePaths = "chargebackCase")
    Optional<Dispute> findById(UUID id);
}
