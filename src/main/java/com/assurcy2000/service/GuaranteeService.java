package com.assurcy2000.service;

import com.assurcy2000.dao.GuaranteeDao;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Guarantee;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.security.Policy;

import java.util.List;

/**
 * Garanties (responsabilité civile, vol, incendie…). Pour l'instant : lecture du catalogue.
 * La gestion par l'agent (créer, modifier, supprimer) viendra avec la partie 2.4.
 */
public class GuaranteeService {

    private final GuaranteeDao guaranteeDao;
    private final TransactionRunner tx;

    public GuaranteeService() { this(new GuaranteeDao(), new HibernateTransactionRunner()); }

    public GuaranteeService(GuaranteeDao guaranteeDao, TransactionRunner tx) {
        this.guaranteeDao = guaranteeDao;
        this.tx = tx;
    }

    public List<Guarantee> listActive(SessionUser actor) {
        Policy.requireLoggedIn(actor);
        return tx.inTransaction(guaranteeDao::findAllActive);
    }
}
