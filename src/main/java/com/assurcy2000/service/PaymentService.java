package com.assurcy2000.service;

import com.assurcy2000.dao.PaymentDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.model.Payment;
import com.assurcy2000.model.enums.PaymentMethod;
import com.assurcy2000.model.enums.PaymentStatus;
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

    public PaymentService() { this(new PaymentDao(), new ContractService(), new UserDao()); }

    public PaymentService(PaymentDao paymentDao, ContractService contractService, UserDao userDao) {
        this.paymentDao = paymentDao;
        this.contractService = contractService;
        this.userDao = userDao;
    }

    public List<Payment> listFor(SessionUser actor) {
        if (Policy.can(actor, PAYMENT_READ_ALL)) return paymentDao.findAll();
        Policy.require(actor, PAYMENT_READ_OWN);
        return paymentDao.findByClient(actor.id());
    }

    public List<Payment> listByContract(SessionUser actor, Long contractId) {
        contractService.get(actor, contractId);
        return paymentDao.findByContract(contractId);
    }

    public Payment markPaid(SessionUser actor, Long paymentId, PaymentMethod method) {
        Policy.require(actor, PAYMENT_WRITE);
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
    }
}
