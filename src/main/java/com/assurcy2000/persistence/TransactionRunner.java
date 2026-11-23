package com.assurcy2000.persistence;

import java.util.function.Supplier;

/**
 * Délimite une transaction autour d'un cas d'usage complet.
 * Ce sont les services qui l'utilisent : tout ce qui est fait dans {@code work}
 * (plusieurs appels de DAO) est validé ensemble, ou annulé ensemble.
 *
 * Interface pour pouvoir passer un runner « direct » dans les tests, sans base de données.
 */
public interface TransactionRunner {

    <R> R inTransaction(Supplier<R> work);

    default void run(Runnable work) {
        inTransaction(() -> { work.run(); return null; });
    }
}
