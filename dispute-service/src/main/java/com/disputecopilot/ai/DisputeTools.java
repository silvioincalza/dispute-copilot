package com.disputecopilot.ai;

import com.disputecopilot.domain.model.ChargebackCase;
import com.disputecopilot.domain.model.Transaction;
import com.disputecopilot.domain.service.DisputeService;
import com.disputecopilot.domain.service.TransactionService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DisputeTools {

    private final TransactionService transactionService;
    private final DisputeService disputeService;
    private final HybridPolicyRetriever hybridPolicyRetriever;

    @Tool(name = "getTransactionHistory", description = "Return the most recent transactions for a customer identifier")
    public List<Transaction> getTransactionHistory(String customerId, Integer limit) {
        return transactionService.getTransactionHistory(customerId, limit == null ? 5 : limit);
    }

    @Tool(name = "getTransactionDetails", description = "Return the full details for a specific transaction identifier")
    public Transaction getTransactionDetails(String transactionId) {
        return transactionService.getTransactionDetails(UUID.fromString(transactionId));
    }

    @Tool(name = "checkScaStatus", description = "Return the SCA and 3DS authentication details for a transaction")
    public String checkScaStatus(String transactionId) {
        var transaction = transactionService.getTransactionDetails(UUID.fromString(transactionId));
        return "SCA status=%s, 3DS version=%s, transaction status=%s".formatted(
                transaction.getScaStatus(),
                transaction.getThreeDsVersion(),
                transaction.getStatus()
        );
    }

    @Tool(name = "draftChargebackCase", description = "Create a draft chargeback case that requires human confirmation before filing")
    public ChargebackCase draftChargebackCase(String disputeId, String reasonCode, String evidenceSummary) {
        return disputeService.draftChargebackCase(UUID.fromString(disputeId), reasonCode, evidenceSummary);
    }

    @Tool(name = "searchPolicyDocuments", description = "Search dispute policy documents and return matching citations")
    public List<HybridPolicyRetriever.RetrievedPolicyDocument> searchPolicyDocuments(String query) {
        return hybridPolicyRetriever.retrieve(query, 5);
    }
}
