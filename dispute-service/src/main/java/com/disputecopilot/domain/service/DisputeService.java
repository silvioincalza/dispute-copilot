package com.disputecopilot.domain.service;

import com.disputecopilot.api.dto.ConfirmActionRequest;
import com.disputecopilot.api.dto.CreateDisputeRequest;
import com.disputecopilot.domain.model.ChargebackCase;
import com.disputecopilot.domain.model.Dispute;
import com.disputecopilot.domain.model.DisputeStatus;
import com.disputecopilot.domain.repository.ChargebackCaseRepository;
import com.disputecopilot.domain.repository.DisputeRepository;
import com.disputecopilot.domain.repository.TransactionRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
@Transactional
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final ChargebackCaseRepository chargebackCaseRepository;
    private final TransactionRepository transactionRepository;

    @Transactional(readOnly = true)
    public List<Dispute> findAll() {
        return disputeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Dispute findById(UUID id) {
        return disputeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Dispute not found: " + id));
    }

    public Dispute create(CreateDisputeRequest request) {
        var transaction = transactionRepository.findById(request.transactionId())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "Unknown transaction: " + request.transactionId()));
        if (!transaction.getCustomerId().equals(request.customerId())) {
            throw new ResponseStatusException(BAD_REQUEST, "Transaction does not belong to customer " + request.customerId());
        }

        var dispute = Dispute.builder()
                .transactionId(request.transactionId())
                .customerId(request.customerId())
                .status(DisputeStatus.OPEN)
                .reason(request.reason())
                .reasonCode(request.reasonCode())
                .agentNotes(request.agentNotes())
                .build();

        return disputeRepository.save(dispute);
    }

    public ChargebackCase draftChargebackCase(UUID disputeId, String reasonCode, String evidenceSummary) {
        var dispute = findById(disputeId);
        var chargebackCase = chargebackCaseRepository.findByDisputeId(disputeId)
                .orElseGet(() -> ChargebackCase.builder()
                        .disputeId(disputeId)
                        .build());

        chargebackCase.setVisaMastercardReasonCode(reasonCode);
        chargebackCase.setEvidenceSummary(evidenceSummary);
        chargebackCase.setPendingConfirmation(true);
        chargebackCase.setFiledAt(null);

        var saved = chargebackCaseRepository.save(chargebackCase);
        dispute.setChargebackCase(saved);
        if (dispute.getStatus() == DisputeStatus.OPEN) {
            dispute.setStatus(DisputeStatus.UNDER_REVIEW);
        }
        dispute.setAgentNotes(appendNote(dispute.getAgentNotes(), "Drafted chargeback case " + saved.getId()));
        disputeRepository.save(dispute);
        return saved;
    }

    public Dispute confirmAction(UUID disputeId, ConfirmActionRequest request) {
        var dispute = findById(disputeId);
        var chargebackCase = chargebackCaseRepository.findByDisputeId(disputeId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No chargeback case for dispute: " + disputeId));

        if (request == null || request.confirmed() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Request body with explicit 'confirmed' field is required");
        }
        boolean confirmed = request.confirmed();
        chargebackCase.setPendingConfirmation(false);
        if (confirmed) {
            chargebackCase.setFiledAt(Instant.now());
            dispute.setStatus(DisputeStatus.UNDER_REVIEW);
            dispute.setAgentNotes(appendNote(dispute.getAgentNotes(), "Chargeback case confirmed and filed."));
        }
        else {
            dispute.setStatus(DisputeStatus.WITHDRAWN);
            dispute.setAgentNotes(appendNote(dispute.getAgentNotes(), "Chargeback case confirmation declined."));
        }

        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            dispute.setAgentNotes(appendNote(dispute.getAgentNotes(), request.notes()));
        }

        chargebackCaseRepository.save(chargebackCase);
        return disputeRepository.save(dispute);
    }

    @Transactional(readOnly = true)
    public List<ChargebackCase> findPendingActions() {
        return chargebackCaseRepository.findByPendingConfirmationTrueOrderByIdDesc();
    }

    private String appendNote(String existing, String note) {
        if (note == null || note.isBlank()) {
            return existing;
        }
        return existing == null || existing.isBlank() ? note : existing + System.lineSeparator() + note;
    }
}
