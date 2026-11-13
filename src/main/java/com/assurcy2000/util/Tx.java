package com.assurcy2000.util;

import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.function.Supplier;

public final class Tx {

    private Tx() {}

    public static <T> T call(Supplier<T> work) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        Transaction tx = session.beginTransaction();
        try {
            T result = work.get();
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        }
    }

    public static void run(Runnable work) {
        call(() -> {
            work.run();
            return null;
        });
    }
}
