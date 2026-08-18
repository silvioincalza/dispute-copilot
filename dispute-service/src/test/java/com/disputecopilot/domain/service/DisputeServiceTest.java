package com.disputecopilot.domain.service;

import com.disputecopilot.api.dto.ConfirmActionRequest;
import com.disputecopilot.api.dto.CreateDisputeRequest;
import com.disputecopilot.domain.model.*;
import com.disputecopilot.domain.repository.ChargebackCaseRepository;
import com.disputecopilot.domain.repository.DisputeRepository;
import com.disputecopilot.domain.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {

    @Mock private DisputeRepository disputeRepository;
    @Mock private ChargebackCaseRepository chargebackCaseRepository;
    @Mock private TransactionRepository transactionRepository;

    @InjectMocks
    private DisputeService disputeService;

    private UUID disputeId;
    private Dispute dispute;

    @BeforeEach
    void setUp() {
        disputeId = UUID.randomUUID();
        dispute = new Dispute();
        dispute.setId(disputeId);
        dispute.setStatus(DisputeStatus.OPEN);
        dispute.setReason("Unauthorized transaction");
        dispute.setCreatedAt(Instant.now());
        dispute.setUpdatedAt(Instant.now());
    }

    @Test
    void findAll_returnsList() {
        when(disputeRepository.findAll()).thenReturn(List.of(dispute));
        assertThat(disputeService.findAll()).hasSize(1);
    }

    @Test
    void findById_returnsDispute() {
        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));
        var result = disputeService.findById(disputeId);
        assertThat(result.getId()).isEqualTo(disputeId);
        assertThat(result.getStatus()).isEqualTo(DisputeStatus.OPEN);
    }

    @Test
    void draftChargebackCase_createsPendingCase() {
        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));
        when(chargebackCaseRepository.findByDisputeId(disputeId)).thenReturn(Optional.empty());
        when(chargebackCaseRepository.save(any())).thenAnswer(inv -> {
            ChargebackCase cc = inv.getArgument(0);
            cc.setId(UUID.randomUUID());
            return cc;
        });
        when(disputeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = disputeService.draftChargebackCase(disputeId, "4853", "Customer never received goods");

        assertThat(result.isPendingConfirmation()).isTrue();
        assertThat(result.getVisaMastercardReasonCode()).isEqualTo("4853");
        assertThat(result.getEvidenceSummary()).contains("never received");
        verify(chargebackCaseRepository).save(any(ChargebackCase.class));
    }

    @Test
    void confirmAction_setsPendingConfirmationFalse() {
        var chargebackCase = new ChargebackCase();
        chargebackCase.setId(UUID.randomUUID());
        chargebackCase.setDisputeId(disputeId);
        chargebackCase.setPendingConfirmation(true);
        chargebackCase.setVisaMastercardReasonCode("4853");

        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));
        when(chargebackCaseRepository.findByDisputeId(disputeId)).thenReturn(Optional.of(chargebackCase));
        when(chargebackCaseRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(disputeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var confirmRequest = new ConfirmActionRequest(true, "looks good");
        var result = disputeService.confirmAction(disputeId, confirmRequest);

        assertThat(result.getStatus()).isEqualTo(DisputeStatus.UNDER_REVIEW);
        assertThat(chargebackCase.isPendingConfirmation()).isFalse();
    }

    @Test
    void findPendingActions_returnsPendingCases() {
        var cc = new ChargebackCase();
        cc.setId(UUID.randomUUID());
        cc.setPendingConfirmation(true);
        cc.setDisputeId(disputeId);
        when(chargebackCaseRepository.findByPendingConfirmationTrueOrderByIdDesc()).thenReturn(List.of(cc));

        var result = disputeService.findPendingActions();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isPendingConfirmation()).isTrue();
    }

    @Test
    void confirmAction_throwsBadRequest_whenNullBody() {
        when(disputeRepository.findById(disputeId)).thenReturn(Optional.of(dispute));
        var chargebackCase = new ChargebackCase();
        chargebackCase.setId(UUID.randomUUID());
        chargebackCase.setDisputeId(disputeId);
        chargebackCase.setPendingConfirmation(true);
        when(chargebackCaseRepository.findByDisputeId(disputeId)).thenReturn(Optional.of(chargebackCase));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> disputeService.confirmAction(disputeId, null))
            .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
    }
}
