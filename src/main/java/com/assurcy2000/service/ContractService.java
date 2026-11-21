package com.assurcy2000.service;

import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.security.Policy;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;

import java.util.List;
import java.util.Map;

import static com.assurcy2000.security.Permission.*;
public class ContractService {

    private final ContractDao contractDao;

    public ContractService() { this(new ContractDao()); }

    public ContractService(ContractDao contractDao) { this.contractDao = contractDao; }

    /**
     * Client can only read his contracts, agent & admins can read any clients'  contracts
     */
    public List<Contract> listFor(SessionUser actor) {
        if (Policy.can(actor, CONTRACT_READ_ALL)) return contractDao.findAll();
        Policy.require(actor, CONTRACT_READ_OWN);
        return contractDao.findByClient(actor.id());
    }

    /**
     * A client can only read his own contracts, and not find someone else's contracts through their ids
     */
    public Contract get(SessionUser actor, Long id) {
        Contract contract = contractDao.findById(id)
                .orElseThrow(() -> new NotFoundException("Contrat introuvable."));
        if (Policy.can(actor, CONTRACT_READ_ALL)) return contract;
        if (Policy.can(actor, CONTRACT_READ_OWN) && contract.getClient().getId().equals(actor.id())) return contract;
        throw new NotFoundException("Contrat introuvable.");
    }

    public List<Contract> listByClient(SessionUser actor, Long clientId) {
        Policy.require(actor, CLIENT_READ);
        Policy.require(actor, CONTRACT_READ_ALL);
        return contractDao.findByClient(clientId);
    }

    public List<Contract> search(SessionUser actor, String keyword) {
        Policy.require(actor, CONTRACT_READ_ALL);
        if (keyword == null || keyword.isBlank()) return contractDao.findAll();
        return contractDao.search(keyword);
    }

    public Contract suspend(SessionUser actor, Long id) {
        Policy.require(actor, CONTRACT_WRITE);
        Contract contract = get(actor, id);
        if (contract.getStatus() != ContractStatus.ACTIF) {
            throw new ValidationException(Map.of("status", "Seul un contrat actif peut être suspendu."));
        }
        contract.setStatus(ContractStatus.SUSPENDU);
        return contractDao.update(contract);
    }
}
