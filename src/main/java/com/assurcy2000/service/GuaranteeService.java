package com.assurcy2000.service;

import com.assurcy2000.dao.GuaranteeDao;
import com.assurcy2000.dto.GuaranteeRow;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.request.GuaranteeRequest;
import com.assurcy2000.model.Guarantee;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.security.Policy;
import com.assurcy2000.service.exceptions.NotFoundException;
import com.assurcy2000.service.exceptions.ValidationException;
import com.assurcy2000.util.Strings;
import com.assurcy2000.util.Validation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.assurcy2000.security.Permission.GUARANTEE_MANAGE;

/**
 * Garanties (section 2.4) : responsabilité civile, vol, incendie…
 * Lecture du catalogue pour tout utilisateur connecté ; création, modification et suppression par l'agent.
 *
 * Le format des saisies (longueurs, montants) est vérifié par la servlet avec Validation ;
 * ici, les règles qui demandent la base : nom unique, garantie utilisée par un contrat.
 */
public class GuaranteeService {

    private final GuaranteeDao guaranteeDao;
    private final TransactionRunner tx;

    public GuaranteeService() { this(new GuaranteeDao(), new HibernateTransactionRunner()); }

    public GuaranteeService(GuaranteeDao guaranteeDao, TransactionRunner tx) {
        this.guaranteeDao = guaranteeDao;
        this.tx = tx;
    }

    /** Garanties actives (formulaire de contrat, types d'assurance). */
    public List<Guarantee> listActive(SessionUser actor) {
        Policy.requireLoggedIn(actor);
        return tx.inTransaction(guaranteeDao::findAllActive);
    }

    /** Page de gestion de l'agent : toutes les garanties, avec le nombre de contrats de chacune. */
    public List<GuaranteeRow> listForAgent(SessionUser actor) {
        Policy.require(actor, GUARANTEE_MANAGE);
        return tx.inTransaction(() -> {
            Map<Long, Long> counts = guaranteeDao.countContractsByGuarantee();
            return guaranteeDao.findAllOrderByName().stream()
                    .map(g -> new GuaranteeRow(g, counts.getOrDefault(g.getId(), 0L)))
                    .toList();
        });
    }

    /** Une garantie, pour le formulaire de modification. */
    public Guarantee get(SessionUser actor, Long id) {
        Policy.require(actor, GUARANTEE_MANAGE);
        return tx.inTransaction(() -> find(id));
    }

    public Guarantee create(SessionUser actor, GuaranteeRequest req) {
        Policy.require(actor, GUARANTEE_MANAGE);
        String name = checkFields(req);
        return tx.inTransaction(() -> {
            checkNameFree(name, null);
            Guarantee g = new Guarantee(name, Strings.blankToNull(req.description()),
                    req.coverageLimit(), req.deductible());
            return guaranteeDao.save(g);
        });
    }

    /**
     * Modifie une garantie. Les contrats qui l'utilisent la voient changer aussi :
     * une garantie est partagée, elle n'est pas copiée dans chaque contrat.
     */
    public Guarantee update(SessionUser actor, Long id, GuaranteeRequest req) {
        Policy.require(actor, GUARANTEE_MANAGE);
        String name = checkFields(req);
        return tx.inTransaction(() -> {
            Guarantee g = find(id);
            checkNameFree(name, id);
            g.setName(name);
            g.setDescription(Strings.blankToNull(req.description()));
            g.setCoverageLimit(req.coverageLimit());
            g.setDeductible(req.deductible());
            return guaranteeDao.update(g);
        });
    }

    /**
     * Supprime une garantie qu'aucun contrat n'utilise (sinon les contrats perdraient une garantie souscrite).
     * La base la retire aussi des types d'assurance qui la proposaient (ON DELETE CASCADE).
     * @return le nom de la garantie supprimée, pour le message de confirmation
     */
    public String delete(SessionUser actor, Long id) {
        Policy.require(actor, GUARANTEE_MANAGE);
        return tx.inTransaction(() -> {
            Guarantee g = find(id);
            if (guaranteeDao.isUsedInContract(id)) {
                throw new ValidationException(Map.of("id",
                        "La garantie « " + g.getName() + " » est utilisée par des contrats : elle ne peut pas être supprimée."));
            }
            guaranteeDao.delete(g);
            return g.getName();
        });
    }

    private Guarantee find(Long id) {
        if (id == null) throw new NotFoundException("Garantie introuvable.");
        return guaranteeDao.findById(id).orElseThrow(() -> new NotFoundException("Garantie introuvable."));
    }

    /**
     * Contrôles sans base, en filet de sécurité si la servlet a laissé passer quelque chose,
     * et règle entre les deux montants : franchise inférieure ou égale au plafond.
     * @return le nom nettoyé
     */
    private static String checkFields(GuaranteeRequest req) {
        Map<String, String> errors = new HashMap<>();
        String name = Validation.guaranteeName(req.name(), errors);
        Validation.description(req.description(), errors);
        if (req.coverageLimit() != null && req.coverageLimit().signum() < 0) {
            errors.put("coverageLimit", "Le plafond doit être positif.");
        }
        if (req.deductible() != null && req.deductible().signum() < 0) {
            errors.put("deductible", "La franchise doit être positive.");
        }
        if (req.coverageLimit() != null && req.deductible() != null
                && req.deductible().compareTo(req.coverageLimit()) > 0) {
            errors.put("deductible", "La franchise ne peut pas dépasser le plafond.");
        }
        if (!errors.isEmpty()) throw new ValidationException(errors);
        return name;
    }

    /** Nom unique, sans tenir compte de la garantie en cours de modification (selfId). */
    private void checkNameFree(String name, Long selfId) {
        guaranteeDao.findByName(name)
                .filter(other -> !other.getId().equals(selfId))
                .ifPresent(other -> {
                    throw new ValidationException(Map.of("name", "Une garantie porte déjà ce nom."));
                });
    }
}
