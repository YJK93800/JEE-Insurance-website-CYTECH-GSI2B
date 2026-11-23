package com.assurcy2000.model;

import com.assurcy2000.model.enums.PaymentMethod;
import com.assurcy2000.model.enums.PaymentStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "paiement")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "montant", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "date_echeance", nullable = false)
    private LocalDate dueDate;

    @Column(name = "date_paiement")
    private LocalDate paymentDate;          // null tant que non payé

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_paiement", length = 20)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private PaymentStatus status = PaymentStatus.EN_ATTENTE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contrat_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enregistre_par_id")
    private User recordedBy;

    protected Payment() { }

    public Payment(BigDecimal amount, LocalDate dueDate, Contract contract) {
        this.amount = amount;
        this.dueDate = dueDate;
        this.contract = contract;
    }

    public Long getId() { return id; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public PaymentMethod getMethod() { return method; }
    public void setMethod(PaymentMethod method) { this.method = method; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public Contract getContract() { return contract; }
    public void setContract(Contract contract) { this.contract = contract; }

    public User getRecordedBy() { return recordedBy; }
    public void setRecordedBy(User recordedBy) { this.recordedBy = recordedBy; }

    @Override
    public String toString() { return "Payment{id=" + id + ", amount=" + amount + ", status=" + status + "}"; }
}
