package com.assurcy2000.persistence;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.function.Supplier;

/**
 * Implémentation Hibernate : ouvre une transaction sur la session du thread courant
 * (hibernate.current_session_context_class = thread). Les DAO récupèrent cette même
 * session via getCurrentSession(), donc tous partagent la transaction.
 * Au commit ou au rollback, Hibernate ferme la session automatiquement.
 */
public class HibernateTransactionRunner implements TransactionRunner {

    @Override
    public <R> R inTransaction(Supplier<R> work) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();

        // Déjà dans une transaction (un service qui appelle un autre service) : on la réutilise.
        if (session.getTransaction().isActive()) {
            return work.get();
        }

        Transaction tx = session.beginTransaction();
        try {
            R result = work.get();
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                try { tx.rollback(); } catch (RuntimeException ignored) { /* on garde l'exception d'origine */ }
            }
            throw e;
        }
    }
}
