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

import java.time.LocalDate;
import java.time.Year;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
            User client = findClient(req.clientId());
            InsuranceType type = findActiveType(req.insuranceTypeId());
            Set<Guarantee> guarantees = guaranteesOfType(type, req.guaranteeIds());

            Contract contract = new Contract(nextNumber(), req.startDate(), req.endDate(),
                    req.premium() != null ? req.premium() : type.getBasePrice(), client, type);
            contract.setStatus(ContractStatus.EN_ATTENTE);
            contract.setAgent(userDao.getReference(actor.id()));
            contract.setGuarantees(guarantees);
            return contractDao.save(contract);
        });
    }

    /**
     * Modifie un contrat (même formulaire que la création).
     * - Dates, prime et garanties : toujours modifiables (sauf contrat résilié ou expiré).
     * - Client et type d'assurance : seulement tant que le contrat est EN_ATTENTE.
     * - Statut : jamais ici, uniquement par les actions (activer, suspendre, résilier, renouveler).
     * - Numéro : jamais.
     * Prime vide = prime actuelle conservée.
     */
    public Contract update(SessionUser actor, Long id, CreateContractRequest req) {
        Policy.require(actor, CONTRACT_WRITE);
        return tx.inTransaction(() -> {
            Contract contract = contractDao.findById(id)
                    .orElseThrow(() -> new NotFoundException("Contrat introuvable."));

            if (contract.getStatus() == ContractStatus.RESILIE || contract.getStatus() == ContractStatus.EXPIRE) {
                throw new ValidationException(Map.of("status",
                        "Un contrat " + contract.getStatus().getLabel().toLowerCase() + " ne peut plus être modifié."));
            }

            boolean clientChanged = !contract.getClient().getId().equals(req.clientId());
            boolean typeChanged = !contract.getInsuranceType().getId().equals(req.insuranceTypeId());
            if (contract.getStatus() != ContractStatus.EN_ATTENTE && (clientChanged || typeChanged)) {
                Map<String, String> errors = new LinkedHashMap<>();
                String msg = "Modifiable seulement tant que le contrat est en attente.";
                if (clientChanged) errors.put("clientId", msg);
                if (typeChanged) errors.put("insuranceTypeId", msg);
                throw new ValidationException(errors);
            }
            if (clientChanged) contract.setClient(findClient(req.clientId()));
            if (typeChanged) contract.setInsuranceType(findActiveType(req.insuranceTypeId()));

            contract.setGuarantees(guaranteesOfType(contract.getInsuranceType(), req.guaranteeIds()));
            contract.setStartDate(req.startDate());
            contract.setEndDate(req.endDate());
            if (req.premium() != null) contract.setPremium(req.premium());
            return contractDao.update(contract);
        });
    }

    private User findClient(Long clientId) {
        return userDao.findById(clientId)
                .filter(u -> Role.CLIENT.equals(u.getRole().getName()))
                .orElseThrow(() -> new ValidationException(Map.of("clientId", "Client introuvable.")));
    }

    private InsuranceType findActiveType(Long typeId) {
        return insuranceTypeDao.findById(typeId)
                .filter(InsuranceType::isActive)
                .orElseThrow(() -> new ValidationException(Map.of("insuranceTypeId", "Type d'assurance introuvable ou désactivé.")));
    }

    /** Les garanties choisies, à condition qu'elles soient toutes proposées par le type (sinon erreur sur le formulaire). */
    private Set<Guarantee> guaranteesOfType(InsuranceType type, Set<Long> guaranteeIds) {
        Set<Guarantee> guarantees = new HashSet<>();
        for (Guarantee g : guaranteeDao.findByInsuranceType(type.getId())) {
            if (guaranteeIds.contains(g.getId())) guarantees.add(g);
        }
        if (guarantees.size() != guaranteeIds.size()) {
            throw new ValidationException(Map.of("guaranteeIds",
                    "Certaines garanties choisies ne sont pas proposées pour « " + type.getName() + " »."));
        }
        return guarantees;
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

    /** Contrat issu du renouvellement de celui-ci, s'il existe (même règle d'accès que get). */
    public Optional<Contract> findRenewal(SessionUser actor, Long id) {
        return tx.inTransaction(() -> {
            Contract contract = get(actor, id);
            return contractDao.findRenewalOf(contract.getId());
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

    /* ------------------------------------------------------------------
     * Actions de statut (boutons de la liste des contrats)
     *   EN_ATTENTE ─activer─▶ ACTIF ─suspendre─▶ SUSPENDU ─activer─▶ ACTIF
     *   EN_ATTENTE / ACTIF / SUSPENDU ─résilier─▶ RESILIE (définitif)
     *   ACTIF / EXPIRE ─renouveler─▶ nouveau contrat EN_ATTENTE lié à l'ancien
     * ------------------------------------------------------------------ */

    /** Active un contrat en attente ou suspendu, à condition que sa période ne soit pas terminée. */
    public Contract activate(SessionUser actor, Long id) {
        Policy.require(actor, CONTRACT_WRITE);
        return tx.inTransaction(() -> {
            Contract contract = get(actor, id);
            if (contract.getStatus() != ContractStatus.EN_ATTENTE && contract.getStatus() != ContractStatus.SUSPENDU) {
                throw new ValidationException(Map.of("status", "Seul un contrat en attente ou suspendu peut être activé."));
            }
            if (contract.getEndDate().isBefore(LocalDate.now())) {
                throw new ValidationException(Map.of("status",
                        "La période de ce contrat est terminée : renouvelez-le plutôt que de l'activer."));
            }
            contract.setStatus(ContractStatus.ACTIF);
            return contractDao.update(contract);
        });
    }

    /**
     * Résilie un contrat (définitif). La date doit être dans la période du contrat
     * (contrainte chk_contrat_date_resil) et le motif est obligatoire.
     */
    public Contract terminate(SessionUser actor, Long id, LocalDate date, String reason) {
        Policy.require(actor, CONTRACT_WRITE);
        return tx.inTransaction(() -> {
            Contract contract = get(actor, id);
            if (!isTerminable(contract)) {
                throw new ValidationException(Map.of("status", "Ce contrat est déjà résilié ou expiré."));
            }
            if (date.isBefore(contract.getStartDate()) || date.isAfter(contract.getEndDate())) {
                throw new ValidationException(Map.of("terminationDate",
                        "La date de résiliation doit être comprise dans la période du contrat."));
            }
            contract.setStatus(ContractStatus.RESILIE);
            contract.setTerminationDate(date);
            contract.setTerminationReason(reason);
            return contractDao.update(contract);
        });
    }

    public static boolean isTerminable(Contract contract) {
        return contract.getStatus() == ContractStatus.EN_ATTENTE
                || contract.getStatus() == ContractStatus.ACTIF
                || contract.getStatus() == ContractStatus.SUSPENDU;
    }

    /**
     * Renouvelle un contrat actif ou expiré : nouveau contrat EN_ATTENTE pour la période suivante
     * (début = lendemain de la fin, durée 1 an), même client, type, prime et garanties encore proposées.
     * Un contrat ne se renouvelle qu'une fois.
     */
    public Contract renew(SessionUser actor, Long id) {
        Policy.require(actor, CONTRACT_WRITE);
        return tx.inTransaction(() -> {
            Contract previous = get(actor, id);
            if (previous.getStatus() != ContractStatus.ACTIF && previous.getStatus() != ContractStatus.EXPIRE) {
                throw new ValidationException(Map.of("status", "Seul un contrat actif ou expiré peut être renouvelé."));
            }
            contractDao.findRenewalOf(previous.getId()).ifPresent(r -> {
                throw new ValidationException(Map.of("status",
                        "Ce contrat a déjà été renouvelé (" + r.getNumber() + ")."));
            });
            InsuranceType type = previous.getInsuranceType();
            if (!type.isActive()) {
                throw new ValidationException(Map.of("status",
                        "Le type « " + type.getName() + " » n'est plus proposé : créez un nouveau contrat."));
            }

            // Garanties de l'ancien contrat encore proposées par le type
            Set<Guarantee> guarantees = new HashSet<>();
            for (Guarantee g : guaranteeDao.findByInsuranceType(type.getId())) {
                if (previous.getGuarantees().contains(g)) guarantees.add(g);
            }

            LocalDate start = previous.getEndDate().plusDays(1);
            Contract renewal = new Contract(nextNumber(), start, start.plusYears(1).minusDays(1),
                    previous.getPremium(), previous.getClient(), type);
            renewal.setStatus(ContractStatus.EN_ATTENTE);
            renewal.setAgent(userDao.getReference(actor.id()));
            renewal.setPreviousContract(previous);
            renewal.setGuarantees(guarantees);
            return contractDao.save(renewal);
        });
    }

    /**
     * Tâche système (ContractExpirationListener) : les contrats actifs ou suspendus dont la date de fin
     * est passée deviennent EXPIRE. Pas d'utilisateur connecté, donc pas de vérification de droits.
     * @return nombre de contrats expirés
     */
    public int expireEndedContracts() {
        return tx.inTransaction(() -> contractDao.expireEndedBefore(LocalDate.now()));
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
