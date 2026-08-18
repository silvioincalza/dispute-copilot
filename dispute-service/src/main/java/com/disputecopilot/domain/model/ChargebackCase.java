package com.disputecopilot.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chargeback_cases")
public class ChargebackCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, name = "dispute_id")
    private UUID disputeId;

    @Column(nullable = false, name = "visa_mastercard_reason_code")
    private String visaMastercardReasonCode;

    @Column(nullable = false, length = 4000, name = "evidence_summary")
    private String evidenceSummary;

    @Column(name = "filed_at")
    private Instant filedAt;

    @Column(nullable = false, name = "pending_confirmation")
    private boolean pendingConfirmation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispute_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Dispute dispute;
}
