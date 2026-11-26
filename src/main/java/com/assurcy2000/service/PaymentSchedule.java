package com.assurcy2000.service;

import com.assurcy2000.dao.PaymentDao;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.Payment;
import com.assurcy2000.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Échéancier d'un contrat : la prime est mensuelle, donc une échéance par mois de la période.
 *   - 1re échéance à la date de début, puis le même jour chaque mois (début.plusMonths(i)),
 *     tant que la date ne dépasse pas la fin du contrat.
 *     Ex. du 01/01/2026 au 31/12/2026 : 12 échéances, du 01/01 au 01/12.
 *   - Montant de chaque échéance = prime du contrat.
 *
 * Appelé par ContractService, dans sa transaction (aucune vérification de droits ici) :
 *   création / renouvellement -> generate ; modification -> resync ; résiliation -> cancelAfter.
 * Les échéances déjà payées ou annulées ne sont jamais modifiées.
 */
public class PaymentSchedule {

    private final PaymentDao paymentDao;

    public PaymentSchedule(PaymentDao paymentDao) {
        this.paymentDao = paymentDao;
    }

    /** Dates d'échéance d'une période : début, début + 1 mois… jusqu'à la date de fin incluse. */
    public static List<LocalDate> dueDates(LocalDate start, LocalDate end) {
        List<LocalDate> dates = new ArrayList<>();
        // plusMonths(i) depuis le début (et pas « date précédente + 1 mois ») : un contrat qui commence
        // le 31 a ses échéances le 31 quand le mois en a un, sinon le dernier jour du mois (28/02, 30/04…).
        for (int i = 0; !start.plusMonths(i).isAfter(end); i++) {
            dates.add(start.plusMonths(i));
        }
        return dates;
    }

    /** Crée toutes les échéances d'un nouveau contrat (création ou renouvellement). */
    public void generate(Contract contract) {
        if (!hasPremium(contract)) return; // contrainte chk_paiement_montant : montant > 0
        for (LocalDate date : dueDates(contract.getStartDate(), contract.getEndDate())) {
            paymentDao.save(new Payment(contract.getPremium(), date, contract));
        }
    }

    /**
     * Après une modification du contrat (dates ou prime) : remet l'échéancier en accord.
     *   - échéance à venir (EN_ATTENTE) toujours dans la période : montant = nouvelle prime ;
     *   - échéance non payée (EN_ATTENTE ou EN_RETARD) hors de la nouvelle période : supprimée ;
     *   - date de la nouvelle période sans échéance : échéance créée.
     * Les échéances EFFECTUE et ANNULE restent telles quelles.
     */
    public void resync(Contract contract) {
        Set<LocalDate> wanted = hasPremium(contract)
                ? new HashSet<>(dueDates(contract.getStartDate(), contract.getEndDate()))
                : Set.of();
        Set<LocalDate> covered = new HashSet<>();

        for (Payment p : paymentDao.findByContract(contract.getId())) {
            boolean unpaid = isUnpaid(p);
            if (unpaid && !wanted.contains(p.getDueDate())) {
                paymentDao.delete(p);
                continue;
            }
            if (p.getStatus() == PaymentStatus.EN_ATTENTE) {
                p.setAmount(contract.getPremium());
            }
            covered.add(p.getDueDate());
        }

        for (LocalDate date : dueDates(contract.getStartDate(), contract.getEndDate())) {
            if (wanted.contains(date) && !covered.contains(date)) {
                paymentDao.save(new Payment(contract.getPremium(), date, contract));
            }
        }
    }

    /**
     * Résiliation : les échéances non payées dont la date est après la résiliation sont annulées.
     * Celles d'avant (déjà dues) restent à payer.
     * @return nombre d'échéances annulées
     */
    public int cancelAfter(Contract contract, LocalDate terminationDate) {
        int cancelled = 0;
        for (Payment p : paymentDao.findByContract(contract.getId())) {
            if (isUnpaid(p) && p.getDueDate().isAfter(terminationDate)) {
                p.setStatus(PaymentStatus.ANNULE);
                cancelled++;
            }
        }
        return cancelled;
    }

    /** Échéance encore à payer : EN_ATTENTE ou EN_RETARD. */
    public static boolean isUnpaid(Payment p) {
        return p.getStatus() == PaymentStatus.EN_ATTENTE || p.getStatus() == PaymentStatus.EN_RETARD;
    }

    private static boolean hasPremium(Contract contract) {
        return contract.getPremium() != null && contract.getPremium().compareTo(BigDecimal.ZERO) > 0;
    }
}
