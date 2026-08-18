package com.disputecopilot.api;

import com.disputecopilot.api.dto.ScaDetailsResponse;
import com.disputecopilot.api.dto.TransactionResponse;
import com.disputecopilot.domain.service.TransactionService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/{customerId}")
    public List<TransactionResponse> getTransactionHistory(@PathVariable String customerId,
                                                           @RequestParam(defaultValue = "10") int limit) {
        return transactionService.getTransactionHistory(customerId, limit).stream().map(ApiMapper::toResponse).toList();
    }

    @GetMapping("/{transactionId}/sca")
    public ScaDetailsResponse getScaStatus(@PathVariable UUID transactionId) {
        var transaction = transactionService.getTransactionDetails(transactionId);
        return new ScaDetailsResponse(
                transaction.getId(),
                transaction.getScaStatus().name(),
                transaction.getThreeDsVersion(),
                transaction.getStatus().name()
        );
    }
}
