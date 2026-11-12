package assurcy2000.listener;

import assurcy2000.util.DbConfig;
import assurcy2000.util.HibernateUtil;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.flywaydb.core.Flyway;

@WebListener
public class DatabaseInitListener implements ServletContextListener {

    private boolean hibernateStarted = false;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
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
