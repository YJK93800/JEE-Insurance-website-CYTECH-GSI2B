package com.assurcy2000.listener;

import com.assurcy2000.service.ContractService;
import com.assurcy2000.service.PaymentService;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Expiration automatique des contrats.
 *
 * Problème : un contrat a une date de fin, mais rien ne change son statut quand cette date passe.
 * Sans ce listener, un contrat ACTIF au 31/12 resterait ACTIF pour toujours, et les statistiques
 * « contrats expirés » (section 2.8 du sujet) seraient fausses.
 *
 * Solution : une tâche de fond qui, régulièrement, passe en EXPIRE les contrats ACTIF ou SUSPENDU
 * dont la date de fin est dépassée (la règle elle-même est dans ContractService.expireEndedContracts).
 * Même problème pour les paiements : la même tâche passe EN_RETARD les échéances dépassées et non payées
 * des contrats en cours (PaymentService.markOverdue), d'abord les retards puis les expirations.
 *
 * Fonctionnement :
 *   - Un ServletContextListener est appelé par Tomcat au démarrage de l'application (contextInitialized)
 *     et à son arrêt (contextDestroyed). @WebListener suffit pour que Tomcat le trouve, sans web.xml.
 *   - Au démarrage, on crée un ScheduledExecutorService : un « minuteur » Java qui exécute une tâche
 *     dans un thread séparé, à intervalle régulier, sans bloquer les requêtes HTTP.
 *   - Première exécution 1 minute après le démarrage, puis toutes les heures.
 *     · Pourquoi attendre 1 minute ? Tomcat ne garantit pas l'ordre des @WebListener : on laisse à
 *       DatabaseInitListener le temps d'appliquer les migrations Flyway et de démarrer Hibernate.
 *     · Pourquoi toutes les heures et pas une fois par jour ? Si Tomcat est démarré à 15 h, une tâche
 *       « quotidienne » tournerait à 15 h ; toutes les heures, un contrat est expiré au plus tard
 *       une heure après minuit. La requête est un simple UPDATE : sans rien à expirer, elle ne fait rien.
 *   - À l'arrêt de Tomcat (ou au redéploiement), on arrête le minuteur pour ne pas laisser de thread actif.
 */
@WebListener
public class ContractExpirationListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(ContractExpirationListener.class.getName());

    /** Délai avant la première vérification, le temps que la base soit prête. */
    private static final long FIRST_RUN_DELAY_MINUTES = 1;
    /** Intervalle entre deux vérifications. */
    private static final long PERIOD_MINUTES = 60;

    /** Le minuteur ; gardé en attribut pour pouvoir l'arrêter dans contextDestroyed. */
    private ScheduledExecutorService scheduler;

    /** Appelé par Tomcat une seule fois, au démarrage de l'application. */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ContractService contractService = new ContractService();
        PaymentService paymentService = new PaymentService();

        // Un seul thread suffit : la tâche est courte et ne doit jamais tourner deux fois en même temps.
        scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread t = new Thread(task, "contract-expiration"); // nom visible dans les logs et le débogueur
            t.setDaemon(true); // thread « démon » : il n'empêche pas Tomcat de s'arrêter
            return t;
        });

        // scheduleAtFixedRate(tâche, délai initial, période, unité) : lance la tâche après le délai,
        // puis la relance à chaque période tant que le minuteur n'est pas arrêté.
        scheduler.scheduleAtFixedRate(() -> {
            try {
                // Avant l'expiration : les échéances dépassées d'un contrat qui expire aujourd'hui passent bien en retard
                int late = paymentService.markOverdue();
                if (late > 0) LOG.info(late + " échéance(s) passée(s) en EN_RETARD.");
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Échec du passage en retard des échéances", e);
            }
            try {
                // Ouvre sa propre transaction, comme n'importe quel appel de service
                int expired = contractService.expireEndedContracts();
                if (expired > 0) LOG.info(expired + " contrat(s) passé(s) en EXPIRE.");
            } catch (RuntimeException e) {
                // Important : si une exception sortait d'ici, le ScheduledExecutorService arrêterait
                // définitivement la tâche, sans prévenir. On l'écrit dans les logs et on réessaie à l'heure suivante
                // (ex. base de données momentanément indisponible).
                LOG.log(Level.WARNING, "Échec de l'expiration automatique des contrats", e);
            }
        }, FIRST_RUN_DELAY_MINUTES, PERIOD_MINUTES, TimeUnit.MINUTES);
    }

    /** Appelé par Tomcat à l'arrêt ou au redéploiement de l'application. */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // shutdownNow() annule les exécutions prévues et interrompt celle en cours, s'il y en a une.
        if (scheduler != null) scheduler.shutdownNow();
    }
}
