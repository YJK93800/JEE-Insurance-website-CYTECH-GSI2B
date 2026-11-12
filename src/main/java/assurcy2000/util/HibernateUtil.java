package assurcy2000.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public final class HibernateUtil {

    private static final SessionFactory sessionFactory = build();

    private HibernateUtil() {}

    private static SessionFactory build() {
        return new Configuration()
                .configure() // lit hibernate.cfg.xml
                .setProperty("hibernate.connection.url", DbConfig.url())
                .setProperty("hibernate.connection.username", DbConfig.user())
                .setProperty("hibernate.connection.password", DbConfig.password())
                .buildSessionFactory();
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
