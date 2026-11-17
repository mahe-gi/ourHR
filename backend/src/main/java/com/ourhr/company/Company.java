package com.ourhr.company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "trade_name")
    private String tradeName;

    @Column(name = "pan_number", length = 10, nullable = false, unique = true)
    private String panNumber;

    @Column(name = "tan_number", length = 10, nullable = false, unique = true)
    private String tanNumber;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "pf_establishment_code", length = 20)
    private String pfEstablishmentCode;

    @Column(name = "esic_code", length = 17)
    private String esicCode;

    @Column(name = "lin", length = 20)
    private String lin;

    @Column(name = "signatory_name", nullable = false)
    private String signatoryName;

    @Column(name = "signatory_designation", length = 100, nullable = false)
    private String signatoryDesignation;

    @Column(name = "signatory_signature_url", length = 500)
    private String signatorySignatureUrl;

    @Builder.Default
    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
