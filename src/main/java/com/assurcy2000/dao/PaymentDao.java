package com.assurcy2000.dao;

import com.assurcy2000.model.Payment;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.model.enums.PaymentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class PaymentDao extends BaseDao<Payment, Long> {

    /** Le contrat et son client sont chargés : les pages affichent le numéro de contrat et le nom du client. */
    private static final String SELECT = "select p from Payment p join fetch p.contract c join fetch c.client cl";

    private static final List<PaymentStatus> UNPAID = List.of(PaymentStatus.EN_ATTENTE, PaymentStatus.EN_RETARD);

    public PaymentDao() { super(Payment.class); }

    @Override
    public Optional<Payment> findById(Long id) {
        return query(s -> s.createQuery(SELECT + " where p.id = :id", Payment.class)
                .setParameter("id", id)
                .uniqueResultOptional());
    }

    @Override
    public List<Payment> findAll() {
        return query(s -> s.createQuery(SELECT + " order by p.dueDate desc, p.id desc", Payment.class)
                .getResultList());
    }

    public List<Payment> findByContract(Long contractId) {
        return query(s -> s.createQuery(SELECT + " where c.id = :contractId order by p.dueDate", Payment.class)
                .setParameter("contractId", contractId)
                .getResultList());
    }

    public List<Payment> findByClient(Long clientId) {
        return query(s -> s.createQuery(SELECT + " where cl.id = :clientId order by p.dueDate desc, p.id desc", Payment.class)
                .setParameter("clientId", clientId)
                .getResultList());
    }

    /** Échéances non payées (en attente ou en retard) dont la date est au plus tard {@code until}, les plus anciennes d'abord. */
    public List<Payment> findUnpaidDueUntil(LocalDate until) {
        return query(s -> s.createQuery(
                SELECT + " where p.status in :unpaid and p.dueDate <= :until order by p.dueDate, c.number", Payment.class)
                .setParameter("unpaid", UNPAID)
                .setParameter("until", until)
                .getResultList());
    }

    /**
     * Passe EN_RETARD les échéances EN_ATTENTE dont la date est dépassée, sur les contrats en cours
     * (ACTIF ou SUSPENDU : un contrat pas encore activé n'est pas en retard de paiement).
     * Requête UPDATE en masse : pas de jointure implicite possible, d'où la sous-requête sur les contrats.
     * @return nombre d'échéances passées en retard
     */
    public int markOverdue(LocalDate today) {
        return query(s -> s.createMutationQuery(
                "update Payment p set p.status = :late"
                        + " where p.status = :pending and p.dueDate < :today"
                        + " and p.contract.id in (select c.id from Contract c where c.status in :running)")
                .setParameter("late", PaymentStatus.EN_RETARD)
                .setParameter("pending", PaymentStatus.EN_ATTENTE)
                .setParameter("today", today)
                .setParameter("running", List.of(ContractStatus.ACTIF, ContractStatus.SUSPENDU))
                .executeUpdate());
    }
}
