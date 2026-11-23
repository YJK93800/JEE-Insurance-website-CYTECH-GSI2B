package com.assurcy2000.service;

import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dao.GuaranteeDao;
import com.assurcy2000.dao.InsuranceTypeDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dto.request.CreateContractRequest;
import com.assurcy2000.model.Guarantee;
import com.assurcy2000.model.InsuranceType;
import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;
import com.assurcy2000.dto.request.ContractSearchRequest;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.security.Policy;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;

import java.time.Year;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.assurcy2000.security.Permission.*;
public class ContractService {

    /** Numéros de contrat : CTR-<année>-<compteur sur 4 chiffres>, ex. CTR-2026-0001. */
    static final String NUMBER_PREFIX = "CTR-";

    private final ContractDao contractDao;
    private final UserDao userDao;
    private final InsuranceTypeDao insuranceTypeDao;
    private final GuaranteeDao guaranteeDao;
    private final TransactionRunner tx;

    public ContractService() {
        this(new ContractDao(), new UserDao(), new InsuranceTypeDao(), new GuaranteeDao(), new HibernateTransactionRunner());
    }

    /** Pour les tests qui n'utilisent que les contrats. */
    public ContractService(ContractDao contractDao, TransactionRunner tx) {
        this(contractDao, new UserDao(), new InsuranceTypeDao(), new GuaranteeDao(), tx);
    }

    public ContractService(ContractDao contractDao, UserDao userDao, InsuranceTypeDao insuranceTypeDao,
                           GuaranteeDao guaranteeDao, TransactionRunner tx) {
        this.contractDao = contractDao;
        this.userDao = userDao;
        this.insuranceTypeDao = insuranceTypeDao;
        this.guaranteeDao = guaranteeDao;
        this.tx = tx;
    }

    /**
     * Crée un contrat EN_ATTENTE pour un client. Le format des champs a déjà été vérifié par la servlet ;
     * ici on vérifie ce qui demande la base : client, type actif, garanties proposées par ce type.
     * Prime vide = prix de base du type. L'agent connecté devient le gestionnaire du contrat.
     */
    public Contract create(SessionUser actor, CreateContractRequest req) {
        Policy.require(actor, CONTRACT_WRITE);
        return tx.inTransaction(() -> {
            User client = userDao.findById(req.clientId())
                    .filter(u -> Role.CLIENT.equals(u.getRole().getName()))
                    .orElseThrow(() -> new ValidationException(Map.of("clientId", "Client introuvable.")));

            InsuranceType type = insuranceTypeDao.findById(req.insuranceTypeId())
                    .filter(InsuranceType::isActive)
                    .orElseThrow(() -> new ValidationException(Map.of("typeAssuranceId", "Type d'assurance introuvable ou désactivé.")));

            Set<Guarantee> guarantees = new HashSet<>();
            for (Guarantee g : guaranteeDao.findByInsuranceType(type.getId())) {
                if (req.guaranteeIds().contains(g.getId())) guarantees.add(g);
            }
            if (guarantees.size() != req.guaranteeIds().size()) {
                throw new ValidationException(Map.of("garanties",
                        "Certaines garanties choisies ne sont pas proposées pour « " + type.getName() + " »."));
            }

            Contract contract = new Contract(nextNumber(), req.startDate(), req.endDate(),
                    req.premium() != null ? req.premium() : type.getBasePrice(), client, type);
            contract.setStatus(ContractStatus.EN_ATTENTE);
            contract.setAgent(userDao.getReference(actor.id()));
            contract.setGuarantees(guarantees);
            return contractDao.save(contract);
        });
    }

    /** Numéro suivant de l'année en cours ; la contrainte UNIQUE sur contrat.numero protège des doublons. */
    private String nextNumber() {
        String prefix = NUMBER_PREFIX + Year.now().getValue() + "-";
        int next = contractDao.findMaxNumberWithPrefix(prefix)
                .map(n -> Integer.parseInt(n.substring(prefix.length())) + 1)
                .orElse(1);
        return prefix + String.format("%04d", next);
    }

    /**
     * Client can only read his contracts, agent & admins can read any clients'  contracts
     */
    public List<Contract> listFor(SessionUser actor) {
        if (Policy.readsAll(actor, CONTRACT_READ_ALL, CONTRACT_READ_OWN)) return tx.inTransaction(contractDao::findAll);
        return tx.inTransaction(() -> contractDao.findByClient(actor.id()));
    }

    /**
     * A client can only read his own contracts, and not find someone else's contracts through their ids
     */
    public Contract get(SessionUser actor, Long id) {
        return tx.inTransaction(() -> contractDao.findById(id)
                .filter(c -> Policy.canRead(actor, CONTRACT_READ_ALL, CONTRACT_READ_OWN, c.getClient().getId()))
                .orElseThrow(() -> new NotFoundException("Contrat introuvable.")));
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
