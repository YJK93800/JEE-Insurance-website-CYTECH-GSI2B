package com.assurcy2000.persistence;

import com.assurcy2000.util.DbConfig;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class HibernateUtil {

    private static final SessionFactory sessionFactory = build();

    private HibernateUtil() { }

    private static SessionFactory build() {
        try {
            return new Configuration()
                    .configure() // lit hibernate.cfg.xml
                    .setProperty("hibernate.connection.url", DbConfig.url())
                    .setProperty("hibernate.connection.username", DbConfig.user())
                    .setProperty("hibernate.connection.password", DbConfig.password())
                    .buildSessionFactory();
        } catch (Throwable e) {
            System.err.println("Failed to Initialize Hibernate : " + e);
            throw new ExceptionInInitializerError(e);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        sessionFactory.close();
    }
}