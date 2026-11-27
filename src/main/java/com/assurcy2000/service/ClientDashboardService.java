package com.assurcy2000.service;

import com.assurcy2000.dao.ClaimDao;
import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dao.PaymentDao;
import com.assurcy2000.dto.ClientDashboard;
import com.assurcy2000.dto.PaymentFilter;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Claim;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.Payment;
import com.assurcy2000.model.enums.ClaimStatus;
import com.assurcy2000.model.enums.ContractStatus;
import com.assurcy2000.model.enums.PaymentStatus;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import org.hibernate.Hibernate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static com.assurcy2000.security.Permission.*;
import com.assurcy2000.security.Policy;

/**
 * Tableau de bord du client connecté : un résumé de ses contrats, sinistres et paiements.
 * Lecture seule. Les données sont toujours celles de l'utilisateur de la session (jamais un id venu de l'URL).
 */
public class ClientDashboardService {

    static final int RECENT_CONTRACTS = 3;
    static final int RECENT_PAYMENTS = 5;

    private final ContractDao contractDao;
    private final ClaimDao claimDao;
    private final PaymentDao paymentDao;
    private final TransactionRunner tx;
    private final Clock clock;

    public ClientDashboardService() {
        this(new ContractDao(), new ClaimDao(), new PaymentDao(), new HibernateTransactionRunner(),
                Clock.systemDefaultZone());
    }

    /** clock : « aujourd'hui », fixé dans les tests. */
    public ClientDashboardService(ContractDao contractDao, ClaimDao claimDao, PaymentDao paymentDao,
                                  TransactionRunner tx, Clock clock) {
        this.contractDao = contractDao;
        this.claimDao = claimDao;
        this.paymentDao = paymentDao;
        this.tx = tx;
        this.clock = clock;
    }

    public ClientDashboard forClient(SessionUser actor) {
        // Le tableau de bord client regroupe contrats, sinistres et paiements : il faut lire les trois.
        Policy.require(actor, CONTRACT_READ_OWN);
        Policy.require(actor, CLAIM_READ_OWN);
        Policy.require(actor, PAYMENT_READ_OWN);
        LocalDate today = LocalDate.now(clock);

        return tx.inTransaction(() -> {
            // Tous les contrats avec leurs garanties (une requête) : le plus récent d'abord.
            List<Contract> contracts = contractDao.findByClientWithGuarantees(
                    actor.id(), Arrays.asList(ContractStatus.values()));
            List<Claim> claims = claimDao.findByClient(actor.id());
            List<Payment> payments = paymentDao.findByClient(actor.id());

            int active = count(contracts, ContractStatus.ACTIF);
            int pending = count(contracts, ContractStatus.EN_ATTENTE);
            BigDecimal premiums = contracts.stream()
                    .filter(c -> c.getStatus() == ContractStatus.ACTIF)
                    .map(Contract::getPremium)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int open = (int) claims.stream().filter(ClientDashboardService::isOpen).count();
            Claim lastClaim = claims.stream()
                    .max(Comparator.comparing(Claim::getDeclaredAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .orElse(null);

            Payment next = payments.stream()
                    .filter(p -> p.getStatus() == PaymentStatus.EN_ATTENTE || p.getStatus() == PaymentStatus.EN_RETARD)
                    .min(Comparator.comparing(Payment::getDueDate))
                    .orElse(null);
            List<Payment> recent = payments.stream()
                    .filter(p -> PaymentFilter.HISTORY.accepts(p, today))
                    .sorted(PaymentFilter.HISTORY.order())
                    .limit(RECENT_PAYMENTS)
                    .toList();
            List<Contract> recentContracts = contracts.stream().limit(RECENT_CONTRACTS).toList();

            // La page est affichée après la fin de la transaction : on charge maintenant ce que la JSP lira.
            if (lastClaim != null) Hibernate.initialize(lastClaim.getContract().getInsuranceType());
            if (next != null) Hibernate.initialize(next.getContract().getInsuranceType());
            recent.forEach(p -> Hibernate.initialize(p.getContract().getInsuranceType()));

            return new ClientDashboard(recentContracts, contracts.size(), active, pending, premiums,
                    open, lastClaim, next, recent);
        });
    }

    private static int count(List<Contract> contracts, ContractStatus status) {
        return (int) contracts.stream().filter(c -> c.getStatus() == status).count();
    }

    private static boolean isOpen(Claim c) {
        return c.getStatus() != ClaimStatus.REFUSE && c.getStatus() != ClaimStatus.INDEMNISE;
    }
}
