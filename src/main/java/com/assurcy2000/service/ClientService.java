package com.assurcy2000.service;

import com.assurcy2000.dao.ClaimDao;
import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.ClientDetail;
import com.assurcy2000.dto.ClientSummary;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.security.Policy;
import com.assurcy2000.service.exceptions.NotFoundException;

import java.util.List;
import java.util.Map;

import static com.assurcy2000.security.Permission.*;

/** Consultation des clients par un agent ou l'admin (page /agent/clients). */
public class ClientService {

    private final UserDao userDao;
    private final ContractDao contractDao;
    private final ClaimDao claimDao;
    private final TransactionRunner tx;

    public ClientService() {
        this(new UserDao(), new ContractDao(), new ClaimDao(), new HibernateTransactionRunner());
    }

    public ClientService(UserDao userDao, ContractDao contractDao, ClaimDao claimDao, TransactionRunner tx) {
        this.userDao = userDao;
        this.contractDao = contractDao;
        this.claimDao = claimDao;
        this.tx = tx;
    }

    /** Clients correspondant au mot-clé (vide : tous), avec leur nombre de contrats. */
    public List<ClientSummary> search(SessionUser actor, String keyword) {
        Policy.require(actor, CLIENT_READ);
        return tx.inTransaction(() -> {
            Map<Long, Long> counts = contractDao.countByClient();
            return userDao.searchClients(keyword).stream()
                    .map(u -> new ClientSummary(u, counts.getOrDefault(u.getId(), 0L)))
                    .toList();
        });
    }

    /**
     * Fiche d'un client avec ses contrats et ses sinistres.
     * Un id qui n'est pas celui d'un client (agent, admin, inexistant) donne « introuvable ».
     */
    public ClientDetail getDetail(SessionUser actor, Long clientId) {
        Policy.require(actor, CLIENT_READ);
        Policy.require(actor, CONTRACT_READ_ALL);
        Policy.require(actor, CLAIM_READ_ALL);
        return tx.inTransaction(() -> {
            User client = userDao.findById(clientId)
                    .filter(u -> Role.CLIENT.equals(u.getRole().getName()))
                    .orElseThrow(() -> new NotFoundException("Client introuvable."));
            return new ClientDetail(client, contractDao.findByClient(clientId), claimDao.findByClient(clientId));
        });
    }
}
