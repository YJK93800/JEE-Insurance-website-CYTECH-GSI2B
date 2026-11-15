package com.assurcy2000.dao;

import com.assurcy2000.model.Payment;
import com.assurcy2000.model.enums.PaymentStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class PaymentDao extends BaseDao<Payment, Long> {

    private static final String SELECT = "select p from Payment p join fetch p.contract c";

    public PaymentDao() { super(Payment.class); }

    @Override
    public Optional<Payment> findById(Long id) {
        return query(s -> s.createQuery(SELECT + " where p.id = :id", Payment.class)
                .setParameter("id", id)
                .uniqueResultOptional());
    }

    @Override
    public List<Payment> findAll() {
        return query(s -> s.createQuery(SELECT + " order by p.dueDate desc", Payment.class)
                .getResultList());
    }

    public List<Payment> findByContract(Long contractId) {
        return query(s -> s.createQuery(SELECT + " where c.id = :contractId order by p.dueDate", Payment.class)
                .setParameter("contractId", contractId)
                .getResultList());
    }

    public List<Payment> findByClient(Long clientId) {
        return query(s -> s.createQuery(SELECT + " where c.client.id = :clientId order by p.dueDate desc", Payment.class)
                .setParameter("clientId", clientId)
                .getResultList());
    }

    public List<Payment> findByClientAndStatus(Long clientId, PaymentStatus status) {
        return query(s -> s.createQuery(
                SELECT + " where c.client.id = :clientId and p.status = :status order by p.dueDate desc", Payment.class)
                .setParameter("clientId", clientId)
                .setParameter("status", status)
                .getResultList());
    }

    public List<Payment> findByStatus(PaymentStatus status) {
        return query(s -> s.createQuery(SELECT + " where p.status = :status order by p.dueDate", Payment.class)
                .setParameter("status", status)
                .getResultList());
    }

    public List<Payment> findOverdue(LocalDate today) {
        return query(s -> s.createQuery(
                SELECT + " where p.status = :status and p.dueDate < :today order by p.dueDate", Payment.class)
                .setParameter("status", PaymentStatus.EN_ATTENTE)
                .setParameter("today", today)
                .getResultList());
    }
}
