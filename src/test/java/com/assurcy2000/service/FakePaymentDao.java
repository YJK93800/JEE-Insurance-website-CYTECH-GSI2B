package com.assurcy2000.service;

import com.assurcy2000.dao.PaymentDao;
import com.assurcy2000.model.Payment;
import com.assurcy2000.model.enums.PaymentStatus;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** PaymentDao en mémoire pour les tests : aucune base de données, les ids sont attribués à l'enregistrement. */
class FakePaymentDao extends PaymentDao {

    final List<Payment> rows = new ArrayList<>();
    private long nextId = 1;

    @Override public Payment save(Payment p) {
        setId(p, nextId++);
        rows.add(p);
        return p;
    }
    @Override public Payment update(Payment p) { return p; }
    @Override public void delete(Payment p) { rows.remove(p); }
    @Override public Optional<Payment> findById(Long id) {
        return rows.stream().filter(p -> p.getId().equals(id)).findFirst();
    }
    @Override public List<Payment> findAll() { return List.copyOf(rows); }
    @Override public List<Payment> findByContract(Long contractId) {
        return rows.stream().filter(p -> p.getContract().getId().equals(contractId))
                .sorted(Comparator.comparing(Payment::getDueDate)).toList();
    }
    @Override public List<Payment> findByClient(Long clientId) {
        return rows.stream().filter(p -> p.getContract().getClient().getId().equals(clientId)).toList();
    }
    @Override public List<Payment> findUnpaidDueUntil(LocalDate until) {
        return rows.stream().filter(PaymentSchedule::isUnpaid).filter(p -> !p.getDueDate().isAfter(until))
                .sorted(Comparator.comparing(Payment::getDueDate)).toList();
    }
    @Override public int markOverdue(LocalDate today) {
        int n = 0;
        for (Payment p : rows) {
            if (p.getStatus() == PaymentStatus.EN_ATTENTE && p.getDueDate().isBefore(today)) {
                p.setStatus(PaymentStatus.EN_RETARD);
                n++;
            }
        }
        return n;
    }

    List<Payment> of(Long contractId) { return findByContract(contractId); }

    private static void setId(Payment p, long id) {
        try {
            Field f = Payment.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(p, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
