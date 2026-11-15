package com.assurcy2000.model;

import com.assurcy2000.model.enums.ContractStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "contrat")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero", nullable = false, unique = true, length = 30)
    private String number;

    @Column(name = "date_debut", nullable = false)
    private LocalDate startDate;

    @Column(name = "date_fin", nullable = false)
    private LocalDate endDate;

    @Column(name = "prime", nullable = false, precision = 10, scale = 2)
    private BigDecimal premium;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private ContractStatus status = ContractStatus.EN_ATTENTE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private User client;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_assurance_id", nullable = false)
    private InsuranceType insuranceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private User agent;

    // Renouvellement : le contrat dont celui-ci est la suite
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contrat_precedent_id")
    private Contract previousContract;

    @Column(name = "date_resiliation")
    private LocalDate terminationDate;

    @Column(name = "motif_resiliation")
    private String terminationReason;

    @Column(name = "date_creation", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "date_modification", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    // Garanties souscrites (table contrat_garantie)
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "contrat_garantie",
            joinColumns = @JoinColumn(name = "contrat_id"),
            inverseJoinColumns = @JoinColumn(name = "garantie_id"))
    private Set<Guarantee> guarantees = new HashSet<>();

    protected Contract() { }

    public Contract(String number, LocalDate startDate, LocalDate endDate, BigDecimal premium,
                    User client, InsuranceType insuranceType) {
        this.number = number;
        this.startDate = startDate;
        this.endDate = endDate;
        this.premium = premium;
        this.client = client;
        this.insuranceType = insuranceType;
    }

    public Long getId() { return id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public BigDecimal getPremium() { return premium; }
    public void setPremium(BigDecimal premium) { this.premium = premium; }

    public ContractStatus getStatus() { return status; }
    public void setStatus(ContractStatus status) { this.status = status; }

    public User getClient() { return client; }
    public void setClient(User client) { this.client = client; }

    public InsuranceType getInsuranceType() { return insuranceType; }
    public void setInsuranceType(InsuranceType insuranceType) { this.insuranceType = insuranceType; }

    public User getAgent() { return agent; }
    public void setAgent(User agent) { this.agent = agent; }

    public Contract getPreviousContract() { return previousContract; }
    public void setPreviousContract(Contract previousContract) { this.previousContract = previousContract; }

    public LocalDate getTerminationDate() { return terminationDate; }
    public void setTerminationDate(LocalDate terminationDate) { this.terminationDate = terminationDate; }

    public String getTerminationReason() { return terminationReason; }
    public void setTerminationReason(String terminationReason) { this.terminationReason = terminationReason; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public Set<Guarantee> getGuarantees() { return guarantees; }
    public void setGuarantees(Set<Guarantee> guarantees) { this.guarantees = guarantees; }

    @Override
    public String toString() { return "Contract{id=" + id + ", number='" + number + "', status=" + status + "}"; }
}
