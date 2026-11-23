package com.assurcy2000.service;

import com.assurcy2000.dao.PaymentDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.model.Payment;
import com.assurcy2000.model.enums.PaymentMethod;
import com.assurcy2000.model.enums.PaymentStatus;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.security.Policy;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.assurcy2000.security.Permission.*;

public class PaymentService {

    private final PaymentDao paymentDao;
    private final ContractService contractService;
    private final UserDao userDao;
    private final TransactionRunner tx;

    public PaymentService() {
        this(new PaymentDao(), new ContractService(), new UserDao(), new HibernateTransactionRunner());
    }

    public PaymentService(PaymentDao paymentDao, ContractService contractService, UserDao userDao,
                          TransactionRunner tx) {
        this.paymentDao = paymentDao;
        this.contractService = contractService;
        this.userDao = userDao;
        this.tx = tx;
    }

    public List<Payment> listFor(SessionUser actor) {
        if (Policy.readsAll(actor, PAYMENT_READ_ALL, PAYMENT_READ_OWN)) return tx.inTransaction(paymentDao::findAll);
        return tx.inTransaction(() -> paymentDao.findByClient(actor.id()));
    }

    public List<Payment> listByContract(SessionUser actor, Long contractId) {
        return tx.inTransaction(() -> {
            contractService.get(actor, contractId); // vérifie l'accès au contrat, dans la même transaction
            return paymentDao.findByContract(contractId);
        });
    }

    public Payment markPaid(SessionUser actor, Long paymentId, PaymentMethod method) {
        Policy.require(actor, PAYMENT_WRITE);
        return tx.inTransaction(() -> {
            Payment payment = paymentDao.findById(paymentId)
                    .orElseThrow(() -> new NotFoundException("Paiement introuvable."));
            if (payment.getStatus() == PaymentStatus.EFFECTUE || payment.getStatus() == PaymentStatus.ANNULE) {
                throw new ValidationException(Map.of("status", "Ce paiement ne peut plus être enregistré."));
            }
            payment.setStatus(PaymentStatus.EFFECTUE);
            payment.setPaymentDate(LocalDate.now());
            payment.setMethod(method);
            payment.setRecordedBy(userDao.findById(actor.id()).orElseThrow());
            return paymentDao.update(payment);
        });
    }
}
