package com.disputecopilot.domain.repository;

import com.disputecopilot.domain.model.Transaction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByCustomerIdOrderByTimestampDesc(String customerId, Pageable pageable);
}
