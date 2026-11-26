package com.assurcy2000.dto;

import com.assurcy2000.model.Payment;
import com.assurcy2000.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;

/**
 * Chiffres affichés en haut des pages de paiements, calculés sur tous les paiements visibles
 * (avant le filtre de l'onglet).
 *   totalPaid      somme des paiements effectués
 *   totalDue       somme des échéances dues à ce jour et non payées
 *   lateCount      nombre d'échéances en retard
 *   nextDue        prochaine échéance à venir (null s'il n'y en a pas)
 */
public record PaymentSummary(BigDecimal totalPaid, BigDecimal totalDue, long lateCount, Payment nextDue) {

    public static PaymentSummary of(Collection<Payment> payments, LocalDate today) {
        BigDecimal paid = BigDecimal.ZERO;
        BigDecimal due = BigDecimal.ZERO;
        long late = 0;
        for (Payment p : payments) {
            if (p.getStatus() == PaymentStatus.EFFECTUE) paid = paid.add(p.getAmount());
            if (PaymentFilter.PENDING.accepts(p, today)) due = due.add(p.getAmount());
            if (p.getStatus() == PaymentStatus.EN_RETARD) late++;
        }
        Payment next = payments.stream()
                .filter(p -> PaymentFilter.UPCOMING.accepts(p, today))
                .min(Comparator.comparing(Payment::getDueDate))
                .orElse(null);
        return new PaymentSummary(paid, due, late, next);
    }

    // Getters au format JavaBean pour les JSP (${summary.totalPaid}…) : l'EL ne lit pas les accesseurs de record.
    public BigDecimal getTotalPaid() { return totalPaid; }
    public BigDecimal getTotalDue() { return totalDue; }
    public long getLateCount() { return lateCount; }
    public Payment getNextDue() { return nextDue; }
}
