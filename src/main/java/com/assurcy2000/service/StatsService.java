package com.assurcy2000.service;

import com.assurcy2000.dao.ClaimDao;
import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dao.jdbc.StatsJdbcDao;
import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.dto.StatsView;
import com.assurcy2000.model.Role;
import com.assurcy2000.persistence.HibernateTransactionRunner;
import com.assurcy2000.persistence.TransactionRunner;
import com.assurcy2000.security.Policy;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static com.assurcy2000.security.Permission.STATS_READ;

public class StatsService {

    private final ContractDao contractDao = new ContractDao();
    private final ClaimDao claimDao = new ClaimDao();
    private final UserDao userDao = new UserDao();
    private final StatsJdbcDao jdbcDao = new StatsJdbcDao(); // JDBC : gère sa propre connexion
    private final TransactionRunner tx = new HibernateTransactionRunner();

    public StatsView dashboard(SessionUser actor) {
        Policy.require(actor, STATS_READ);
        return tx.inTransaction(() -> {
            var revenue = jdbcDao.revenueByMonth();
            var indemnities = jdbcDao.indemnitiesByMonth();
            var contracts = jdbcDao.contractsByMonth();
            return new StatsView(
                    userDao.findByRole(Role.CLIENT).size(),
                    jdbcDao.newClients(),
                    jdbcDao.clientsByType(),
                    contractDao.countByStatus(),
                    claimDao.countByStatus(),
                    jdbcDao.activePremiums(),
                    jdbcDao.paymentsReceived(),
                    jdbcDao.indemnitiesPaid(),
                    jdbcDao.averageProcessingDays(),
                    mergeMonths(revenue, indemnities, contracts),
                    revenue, indemnities, contracts);
        });
    }

    @SafeVarargs
    static List<String> mergeMonths(Map<String, ?>... series) {
        TreeSet<String> months = new TreeSet<>();
        for (Map<String, ?> s : series) months.addAll(s.keySet());
        return List.copyOf(months);
    }
}