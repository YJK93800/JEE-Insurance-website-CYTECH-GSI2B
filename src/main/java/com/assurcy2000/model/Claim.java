package com.assurcy2000.model;

import com.assurcy2000.model.enums.ClaimStatus;
import com.assurcy2000.model.enums.ClaimType;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "sinistre")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String number;

    @Column(name = "date_sinistre", nullable = false)
    private LocalDate claimDate;

    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_sinistre", nullable = false, length = 30)
    private ClaimType type;

    @Column(name = "montant_estime", nullable = false, precision = 12, scale = 2)
    private BigDecimal estimatedAmount;

    @Column(name = "montant_indemnise", precision = 12, scale = 2)
    private BigDecimal indemnifiedAmount;

    @Column(name = "date_indemnisation")
    private LocalDate indemnificationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private ClaimStatus status = ClaimStatus.DECLARE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrat_id", nullable = false)
    private Contract contract;

    // Agent qui traite le dossier
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private User agent;

    @Column(name = "date_declaration", insertable = false, updatable = false)
    private LocalDateTime declaredAt;

    @Column(name = "date_modification", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected Claim() { }

    public Claim(String number, LocalDate claimDate, String description, ClaimType type,
                 BigDecimal estimatedAmount, Contract contract) {
        this.number = number;
        this.claimDate = claimDate;
        this.description = description;
        this.type = type;
        this.estimatedAmount = estimatedAmount;
        this.contract = contract;
    }

    public Long getId() { return id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public LocalDate getClaimDate() { return claimDate; }
    public void setClaimDate(LocalDate claimDate) { this.claimDate = claimDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ClaimType getType() { return type; }
    public void setType(ClaimType type) { this.type = type; }

    public BigDecimal getEstimatedAmount() { return estimatedAmount; }
    public void setEstimatedAmount(BigDecimal estimatedAmount) { this.estimatedAmount = estimatedAmount; }

    public BigDecimal getIndemnifiedAmount() { return indemnifiedAmount; }
    public void setIndemnifiedAmount(BigDecimal indemnifiedAmount) { this.indemnifiedAmount = indemnifiedAmount; }

    public LocalDate getIndemnificationDate() { return indemnificationDate; }
    public void setIndemnificationDate(LocalDate indemnificationDate) { this.indemnificationDate = indemnificationDate; }

    public ClaimStatus getStatus() { return status; }
    public void setStatus(ClaimStatus status) { this.status = status; }

    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }

    public User getAgent() { return agent; }
    public void setAgent(User agent) { this.agent = agent; }

    public LocalDateTime getDeclaredAt() { return declaredAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    @Override
    public String toString() { return "Claim{id=" + id + ", number='" + number + "', status=" + status + "}"; }
}
