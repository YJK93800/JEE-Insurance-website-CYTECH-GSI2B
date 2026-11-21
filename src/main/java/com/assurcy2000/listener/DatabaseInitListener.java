package com.assurcy2000.listener;

import com.assurcy2000.util.DbConfig;
import com.assurcy2000.persistence.HibernateUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.flywaydb.core.Flyway;

@WebListener
public class DatabaseInitListener implements ServletContextListener {

    private boolean hibernateStarted = false;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // Flyway met le schéma à jour
        Flyway.configure()
                .dataSource(DbConfig.url(), DbConfig.user(), DbConfig.password())
                .locations("classpath:db/migrations")
                .load()
                .migrate();
        HibernateUtil.getSessionFactory();
        hibernateStarted = true;
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (hibernateStarted) {
            HibernateUtil.getSessionFactory().close();
        }
    }
}