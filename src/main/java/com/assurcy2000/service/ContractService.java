package com.assurcy2000.service;

import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dto.request.ContractSearchRequest;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.security.Policy;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;

import java.util.List;
import java.util.Map;

import static com.assurcy2000.security.Permission.*;
public class ContractService {

    private final ContractDao contractDao;
    private final TransactionRunner tx;

    public ContractService() { this(new ContractDao(), new HibernateTransactionRunner()); }

    public ContractService(ContractDao contractDao, TransactionRunner tx) {
        this.contractDao = contractDao;
        this.tx = tx;
    }

    /**
     * Client can only read his contracts, agent & admins can read any clients'  contracts
     */
    public List<Contract> listFor(SessionUser actor) {
        if (Policy.can(actor, CONTRACT_READ_ALL)) return tx.inTransaction(contractDao::findAll);
        Policy.require(actor, CONTRACT_READ_OWN);
        return tx.inTransaction(() -> contractDao.findByClient(actor.id()));
    }

    /**
     * A client can only read his own contracts, and not find someone else's contracts through their ids
     */
    public Contract get(SessionUser actor, Long id) {
        return tx.inTransaction(() -> {
            Contract contract = contractDao.findById(id)
                    .orElseThrow(() -> new NotFoundException("Contrat introuvable."));
            if (Policy.can(actor, CONTRACT_READ_ALL)) return contract;
            if (Policy.can(actor, CONTRACT_READ_OWN) && contract.getClient().getId().equals(actor.id())) return contract;
            throw new NotFoundException("Contrat introuvable.");
        });
    }

    public List<Contract> listByClient(SessionUser actor, Long clientId) {
        Policy.require(actor, CLIENT_READ);
        Policy.require(actor, CONTRACT_READ_ALL);
        return tx.inTransaction(() -> contractDao.findByClient(clientId));
    }

    /** Recherche réservée aux agents et à l'admin (tous les contrats). */
    public List<Contract> search(SessionUser actor, ContractSearchRequest criteria) {
        Policy.require(actor, CONTRACT_READ_ALL);
        return tx.inTransaction(() -> contractDao.search(criteria));
    }

    public Contract suspend(SessionUser actor, Long id) {
        Policy.require(actor, CONTRACT_WRITE);
        return tx.inTransaction(() -> {
            Contract contract = get(actor, id); // réutilise la transaction en cours
            if (contract.getStatus() != ContractStatus.ACTIF) {
                throw new ValidationException(Map.of("status", "Seul un contrat actif peut être suspendu."));
            }
            contract.setStatus(ContractStatus.SUSPENDU);
            return contractDao.update(contract);
        });
    }
}
