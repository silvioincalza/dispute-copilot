package com.disputecopilot.config;

import com.disputecopilot.ai.PolicyDocumentIngestionService;
import com.disputecopilot.domain.model.ChargebackCase;
import com.disputecopilot.domain.model.Customer;
import com.disputecopilot.domain.model.Dispute;
import com.disputecopilot.domain.model.DisputeStatus;
import com.disputecopilot.domain.model.ScaStatus;
import com.disputecopilot.domain.model.Transaction;
import com.disputecopilot.domain.model.TransactionStatus;
import com.disputecopilot.domain.repository.ChargebackCaseRepository;
import com.disputecopilot.domain.repository.CustomerRepository;
import com.disputecopilot.domain.repository.DisputeRepository;
import com.disputecopilot.domain.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final DisputeRepository disputeRepository;
    private final ChargebackCaseRepository chargebackCaseRepository;
    private final PolicyDocumentIngestionService policyDocumentIngestionService;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedCustomers();
        seedTransactions();
        seedDisputes();
        policyDocumentIngestionService.ingestBundledPolicies();
    }

    private void seedCustomers() {
        if (customerRepository.count() > 0) {
            return;
        }
        customerRepository.saveAll(List.of(
                Customer.builder().name("Alice Johnson").email("alice@example.com").customerId("CUST-1001").build(),
                Customer.builder().name("Bob Smith").email("bob@example.com").customerId("CUST-1002").build(),
                Customer.builder().name("Carla Diaz").email("carla@example.com").customerId("CUST-1003").build()
        ));
    }

    private void seedTransactions() {
        if (transactionRepository.count() > 0) {
            return;
        }
        var base = Instant.now().minus(12, ChronoUnit.DAYS);
        transactionRepository.saveAll(List.of(
                transaction("CUST-1001", "Acme Travel", "899.00", "EUR", base.plus(1, ChronoUnit.DAYS), TransactionStatus.APPROVED, ScaStatus.AUTHENTICATED, "2.2.0"),
                transaction("CUST-1001", "Metro Cab", "24.10", "EUR", base.plus(2, ChronoUnit.DAYS), TransactionStatus.APPROVED, ScaStatus.EXEMPTED, "2.1.0"),
                transaction("CUST-1001", "Nordic Air", "512.75", "EUR", base.plus(3, ChronoUnit.DAYS), TransactionStatus.REVERSED, ScaStatus.AUTHENTICATED, "2.2.0"),
                transaction("CUST-1002", "Blue Electronics", "132.40", "USD", base.plus(4, ChronoUnit.DAYS), TransactionStatus.APPROVED, ScaStatus.NOT_AUTHENTICATED, "1.0.2"),
                transaction("CUST-1002", "City Market", "48.99", "USD", base.plus(5, ChronoUnit.DAYS), TransactionStatus.APPROVED, ScaStatus.EXEMPTED, "2.2.0"),
                transaction("CUST-1002", "Orbital Streaming", "13.99", "USD", base.plus(6, ChronoUnit.DAYS), TransactionStatus.DECLINED, ScaStatus.NOT_AUTHENTICATED, "2.1.0"),
                transaction("CUST-1003", "Green Hotel", "244.00", "GBP", base.plus(7, ChronoUnit.DAYS), TransactionStatus.APPROVED, ScaStatus.AUTHENTICATED, "2.2.0"),
                transaction("CUST-1003", "Rapid Rail", "61.20", "GBP", base.plus(8, ChronoUnit.DAYS), TransactionStatus.APPROVED, ScaStatus.EXEMPTED, "2.2.0"),
                transaction("CUST-1003", "Harbor Dining", "83.11", "GBP", base.plus(9, ChronoUnit.DAYS), TransactionStatus.APPROVED, ScaStatus.AUTHENTICATED, "2.1.0"),
                transaction("CUST-1001", "Cloud Gadgets", "1250.00", "EUR", base.plus(10, ChronoUnit.DAYS), TransactionStatus.APPROVED, ScaStatus.NOT_AUTHENTICATED, "1.0.2")
        ));
    }

    private void seedDisputes() {
        if (disputeRepository.count() > 0) {
            return;
        }
        var transactions = transactionRepository.findAll();
        var disputeOne = Dispute.builder()
                .transactionId(transactions.get(0).getId())
                .customerId(transactions.get(0).getCustomerId())
                .status(DisputeStatus.OPEN)
                .reason("Cardholder claims the merchant did not provide the booked travel service.")
                .reasonCode("13.1")
                .agentNotes("Awaiting supporting itinerary documents.")
                .build();
        var disputeTwo = Dispute.builder()
                .transactionId(transactions.get(3).getId())
                .customerId(transactions.get(3).getCustomerId())
                .status(DisputeStatus.UNDER_REVIEW)
                .reason("Goods reported as defective after delivery.")
                .reasonCode("13.3")
                .agentNotes("Merchant requested additional shipment evidence.")
                .build();
        var disputeThree = Dispute.builder()
                .transactionId(transactions.get(9).getId())
                .customerId(transactions.get(9).getCustomerId())
                .status(DisputeStatus.RESOLVED_WIN)
                .reason("Recurring charge recognized as cancelled subscription.")
                .reasonCode("13.2")
                .agentNotes("Refund credited after representment review.")
                .build();

        disputeRepository.saveAll(List.of(disputeOne, disputeTwo, disputeThree));

        var savedTwo = disputeRepository.findAll().stream()
                .filter(dispute -> dispute.getTransactionId().equals(transactions.get(3).getId()))
                .findFirst()
                .orElseThrow();
        var caseTwo = ChargebackCase.builder()
                .disputeId(savedTwo.getId())
                .visaMastercardReasonCode("13.3")
                .evidenceSummary("Device photos, return authorization log, and merchant correspondence.")
                .filedAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .pendingConfirmation(false)
                .build();
        chargebackCaseRepository.save(caseTwo);
        savedTwo.setChargebackCase(caseTwo);
        disputeRepository.save(savedTwo);
    }

    private Transaction transaction(String customerId,
                                    String merchant,
                                    String amount,
                                    String currency,
                                    Instant timestamp,
                                    TransactionStatus status,
                                    ScaStatus scaStatus,
                                    String threeDsVersion) {
        return Transaction.builder()
                .customerId(customerId)
                .merchantName(merchant)
                .amount(new BigDecimal(amount))
                .currency(currency)
                .timestamp(timestamp)
                .status(status)
                .scaStatus(scaStatus)
                .threeDsVersion(threeDsVersion)
                .build();
    }
}
