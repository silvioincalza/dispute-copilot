package com.disputecopilot.domain.service;

import com.disputecopilot.domain.model.Transaction;
import com.disputecopilot.domain.repository.TransactionRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> getTransactionHistory(String customerId, int limit) {
        return transactionRepository.findByCustomerIdOrderByTimestampDesc(customerId, PageRequest.of(0, Math.max(1, limit)));
    }

    public Transaction getTransactionDetails(UUID transactionId) {
        return transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Transaction not found: " + transactionId));
    }
}
