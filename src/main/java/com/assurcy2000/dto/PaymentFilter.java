package com.assurcy2000.dto;

import com.assurcy2000.model.Payment;
import com.assurcy2000.model.enums.PaymentStatus;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * Onglets des pages de paiements (client et agent), dans ?filter=… :
 *   (aucun) HISTORY  historique : tout ce qui est payé, annulé ou déjà dû — pas les échéances futures
 *   paid    PAID     paiements effectués
 *   pending PENDING  en attente : échéances dues à ce jour et non payées (en retard compris)
 *   due     UPCOMING échéances à venir (date après aujourd'hui, non payées)
 * Un paiement non payé est soit « en attente » soit « à venir » selon sa date, jamais les deux.
 */
public enum PaymentFilter {
    HISTORY("", "Historique"),
    PAID("paid", "Effectués"),
    PENDING("pending", "En attente"),
    UPCOMING("due", "Échéances");

    private final String param;
    private final String label;

    PaymentFilter(String param, String label) {
        this.param = param;
        this.label = label;
    }

    /** Valeur de ?filter= dans l'URL ("" pour l'historique). */
    public String getParam() { return param; }
    public String getLabel() { return label; }

    /** Onglet demandé ; historique si le paramètre est absent ou inconnu. */
    public static PaymentFilter fromParam(String value) {
        for (PaymentFilter f : values()) {
            if (f.param.equals(value)) return f;
        }
        return HISTORY;
    }

    public boolean accepts(Payment p, LocalDate today) {
        boolean unpaid = p.getStatus() == PaymentStatus.EN_ATTENTE || p.getStatus() == PaymentStatus.EN_RETARD;
        boolean dueNow = !p.getDueDate().isAfter(today);
        return switch (this) {
            case HISTORY -> !unpaid || dueNow;
            case PAID -> p.getStatus() == PaymentStatus.EFFECTUE;
            case PENDING -> unpaid && (dueNow || p.getStatus() == PaymentStatus.EN_RETARD);
            case UPCOMING -> unpaid && !dueNow && p.getStatus() != PaymentStatus.EN_RETARD;
        };
    }

    /**
     * Ordre d'affichage : le plus récent d'abord pour l'historique et les paiements effectués,
     * le plus ancien (le plus urgent) d'abord pour ce qui reste à payer.
     */
    public Comparator<Payment> order() {
        Comparator<Payment> byDue = Comparator.comparing(Payment::getDueDate);
        return switch (this) {
            case PENDING, UPCOMING -> byDue;
            case PAID -> Comparator.comparing((Payment p) -> p.getPaymentDate() != null ? p.getPaymentDate() : p.getDueDate())
                    .reversed();
            case HISTORY -> byDue.reversed();
        };
    }
}
