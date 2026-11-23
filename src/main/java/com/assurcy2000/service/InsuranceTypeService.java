package com.assurcy2000.service;

import com.assurcy2000.dao.InsuranceTypeDao;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.InsuranceType;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.service.exceptions.ForbiddenException;

import java.util.List;

/**
 * Types d'assurance (automobile, habitation…). Pour l'instant : lecture du catalogue.
 * La gestion par l'admin (créer, modifier, désactiver) viendra avec la partie 2.3.
 */
public class InsuranceTypeService {

    private final InsuranceTypeDao insuranceTypeDao;
    private final TransactionRunner tx;

    public InsuranceTypeService() { this(new InsuranceTypeDao(), new HibernateTransactionRunner()); }

    public InsuranceTypeService(InsuranceTypeDao insuranceTypeDao, TransactionRunner tx) {
        this.insuranceTypeDao = insuranceTypeDao;
        this.tx = tx;
    }

    /** Types proposés à la souscription (actifs), avec leurs garanties. */
    public List<InsuranceType> listActive(SessionUser actor) {
        if (actor == null) throw new ForbiddenException();
        return tx.inTransaction(insuranceTypeDao::findAllActive);
    }

    /** Tous les types, y compris désactivés (un ancien contrat peut en avoir un) : catalogue lisible par tout utilisateur connecté. */
    public List<InsuranceType> listAll(SessionUser actor) {
        if (actor == null) throw new ForbiddenException();
        return tx.inTransaction(insuranceTypeDao::findAll);
    }
}
