package com.assurcy2000.service;

import com.assurcy2000.dao.GuaranteeDao;
import com.assurcy2000.dao.InsuranceTypeDao;
import com.assurcy2000.dto.InsuranceTypeRow;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.request.InsuranceTypeRequest;
import com.assurcy2000.model.Guarantee;
import com.assurcy2000.model.InsuranceType;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.security.Policy;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.Strings;
import com.assurcy2000.util.Validation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.assurcy2000.security.Permission.INSURANCE_TYPE_MANAGE;

/**
 * Types d'assurance (section 2.3) : automobile, habitation…
 * Lecture du catalogue pour tout utilisateur connecté ; création, modification, activation et suppression par l'admin.
 * Les types sont des lignes de la table type_assurance : en ajouter un ne demande aucun changement de code.
 *
 * Le format des saisies est vérifié par la servlet avec Validation ;
 * ici, les règles qui demandent la base : nom unique, garanties existantes, type utilisé par des contrats.
 */
public class InsuranceTypeService {

    private final InsuranceTypeDao insuranceTypeDao;
    private final GuaranteeDao guaranteeDao;
    private final TransactionRunner tx;

    public InsuranceTypeService() {
        this(new InsuranceTypeDao(), new GuaranteeDao(), new HibernateTransactionRunner());
    }

    public InsuranceTypeService(InsuranceTypeDao insuranceTypeDao, GuaranteeDao guaranteeDao, TransactionRunner tx) {
        this.insuranceTypeDao = insuranceTypeDao;
        this.guaranteeDao = guaranteeDao;
        this.tx = tx;
    }

    /** Types proposés à la souscription (actifs), avec leurs garanties. */
    public List<InsuranceType> listActive(SessionUser actor) {
        Policy.requireLoggedIn(actor);
        return tx.inTransaction(insuranceTypeDao::findAllActive);
    }

    /** Tous les types, y compris désactivés (un ancien contrat peut en avoir un) : catalogue lisible par tout utilisateur connecté. */
    public List<InsuranceType> listAll(SessionUser actor) {
        Policy.requireLoggedIn(actor);
        return tx.inTransaction(insuranceTypeDao::findAll);
    }

    /** Page de gestion de l'admin : tous les types avec leurs garanties et leur nombre de contrats. */
    public List<InsuranceTypeRow> listForAdmin(SessionUser actor) {
        Policy.require(actor, INSURANCE_TYPE_MANAGE);
        return tx.inTransaction(() -> {
            Map<Long, Long> counts = insuranceTypeDao.countContractsByType();
            return insuranceTypeDao.findAll().stream()
                    .map(t -> new InsuranceTypeRow(t, counts.getOrDefault(t.getId(), 0L)))
                    .toList();
        });
    }

    /** Garanties que l'admin peut cocher dans le formulaire (tout le catalogue, par nom). */
    public List<Guarantee> guaranteeChoices(SessionUser actor) {
        Policy.require(actor, INSURANCE_TYPE_MANAGE);
        return tx.inTransaction(guaranteeDao::findAllOrderByName);
    }

    /** Un type avec ses garanties, pour le formulaire de modification. */
    public InsuranceType get(SessionUser actor, Long id) {
        Policy.require(actor, INSURANCE_TYPE_MANAGE);
        return tx.inTransaction(() -> find(id));
    }

    public InsuranceType create(SessionUser actor, InsuranceTypeRequest req) {
        Policy.require(actor, INSURANCE_TYPE_MANAGE);
        String name = checkFields(req);
        return tx.inTransaction(() -> {
            checkNameFree(name, null);
            InsuranceType t = new InsuranceType(name, Strings.blankToNull(req.description()), req.basePrice());
            t.getGuarantees().addAll(findGuarantees(req.guaranteeIds()));
            return insuranceTypeDao.save(t);
        });
    }

    /**
     * Modifie un type. Les contrats existants ne changent pas : leur prime et leurs garanties souscrites
     * sont enregistrées dans le contrat. Une garantie retirée du type n'est plus proposée dans le formulaire
     * de contrat ; un contrat qui l'avait la perd s'il est ensuite modifié par un agent.
     */
    public InsuranceType update(SessionUser actor, Long id, InsuranceTypeRequest req) {
        Policy.require(actor, INSURANCE_TYPE_MANAGE);
        String name = checkFields(req);
        return tx.inTransaction(() -> {
            InsuranceType t = find(id);
            checkNameFree(name, id);
            Set<Guarantee> guarantees = findGuarantees(req.guaranteeIds());
            t.setName(name);
            t.setDescription(Strings.blankToNull(req.description()));
            t.setBasePrice(req.basePrice());
            // modifier la collection chargée (pas la remplacer) : Hibernate ne réécrit que les lignes changées
            t.getGuarantees().retainAll(guarantees);
            t.getGuarantees().addAll(guarantees);
            return insuranceTypeDao.update(t);
        });
    }

    /** Active ou désactive un type : un type désactivé n'est plus proposé pour un nouveau contrat. */
    public InsuranceType setActive(SessionUser actor, Long id, boolean active) {
        Policy.require(actor, INSURANCE_TYPE_MANAGE);
        return tx.inTransaction(() -> {
            InsuranceType t = find(id);
            t.setActive(active);
            return insuranceTypeDao.update(t);
        });
    }

    /**
     * Supprime un type qu'aucun contrat n'utilise (sinon il faut le désactiver).
     * Ses liens avec les garanties sont supprimés par la base (ON DELETE CASCADE), pas les garanties.
     * @return le nom du type supprimé, pour le message de confirmation
     */
    public String delete(SessionUser actor, Long id) {
        Policy.require(actor, INSURANCE_TYPE_MANAGE);
        return tx.inTransaction(() -> {
            InsuranceType t = find(id);
            if (insuranceTypeDao.isUsedByContract(id)) {
                throw new ValidationException(Map.of("id", "Le type « " + t.getName()
                        + " » est utilisé par des contrats : désactivez-le plutôt que de le supprimer."));
            }
            insuranceTypeDao.delete(t);
            return t.getName();
        });
    }

    private InsuranceType find(Long id) {
        if (id == null) throw new NotFoundException("Type d'assurance introuvable.");
        return insuranceTypeDao.findById(id).orElseThrow(() -> new NotFoundException("Type d'assurance introuvable."));
    }

    /**
     * Contrôles sans base, en filet de sécurité si la servlet a laissé passer quelque chose.
     * Au moins une garantie : un contrat doit en avoir une, un type sans garantie ne pourrait pas être souscrit.
     * @return le nom nettoyé
     */
    private static String checkFields(InsuranceTypeRequest req) {
        Map<String, String> errors = new HashMap<>();
        String name = Validation.insuranceTypeName(req.name(), errors);
        Validation.description(req.description(), errors);
        if (req.basePrice() == null || req.basePrice().signum() <= 0) {
            errors.put("basePrice", "Le prix de base doit être un montant positif.");
        }
        if (req.guaranteeIds().isEmpty()) {
            errors.put("guaranteeIds", "Cochez au moins une garantie.");
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
        return name;
    }

    /** Les garanties cochées ; erreur si l'une n'existe pas (supprimée entre-temps, ou id forgé). */
    private Set<Guarantee> findGuarantees(Set<Long> ids) {
        Set<Guarantee> result = new HashSet<>();
        for (Long gid : ids) {
            result.add(guaranteeDao.findById(gid).orElseThrow(() -> new ValidationException(
                    Map.of("guaranteeIds", "Une des garanties cochées n'existe plus. Rechargez la page."))));
        }
        return result;
    }

    /** Nom unique, sans tenir compte du type en cours de modification (selfId). */
    private void checkNameFree(String name, Long selfId) {
        insuranceTypeDao.findByName(name)
                .filter(other -> !other.getId().equals(selfId))
                .ifPresent(other -> {
                    throw new ValidationException(Map.of("name", "Un type d'assurance porte déjà ce nom."));
                });
    }
}
