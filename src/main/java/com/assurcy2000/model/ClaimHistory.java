package com.assurcy2000.model;

import com.assurcy2000.model.enums.ClaimStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Une ligne par changement de statut d'un sinistre (table historique_sinistre). */
@Entity
@Table(name = "historique_sinistre")
public class ClaimHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_statut", length = 20)
    private ClaimStatus oldStatus;          // null à la déclaration

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_statut", nullable = false, length = 20)
    private ClaimStatus newStatus;

    @Column(name = "date_changement", insertable = false, updatable = false)
    private LocalDateTime changedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sinistre_id", nullable = false)
    private Claim claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifie_par_id")
    private User changedBy;

    protected ClaimHistory() { }

    public ClaimHistory(Claim claim, ClaimStatus oldStatus, ClaimStatus newStatus, User changedBy) {
        this.claim = claim;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedBy = changedBy;
    }

    public Long getId() { return id; }
    public ClaimStatus getOldStatus() { return oldStatus; }
    public ClaimStatus getNewStatus() { return newStatus; }
    public LocalDateTime getChangedAt() { return changedAt; }
    public Claim getClaim() { return claim; }
    public User getChangedBy() { return changedBy; }
}
