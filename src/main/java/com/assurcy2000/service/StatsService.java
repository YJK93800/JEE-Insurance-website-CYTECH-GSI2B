package com.assurcy2000.service;

import com.assurcy2000.dao.ClaimDao;
import com.assurcy2000.dao.ContractDao;
import com.assurcy2000.dao.UserDao;
import com.assurcy2000.dao.jdbc.StatsJdbcDao;
import com.assurcy2000.dto.StatsView;
import com.assurcy2000.model.Role;
import com.assurcy2000.security.Policy;
import com.assurcy2000.dto.SessionUser;

import static com.assurcy2000.security.Permission.STATS_READ;

public class StatsService {

    private final ContractDao contractDao = new ContractDao();
    private final ClaimDao claimDao = new ClaimDao();
    private final UserDao userDao = new UserDao();
    private final StatsJdbcDao jdbcDao = new StatsJdbcDao();

    public StatsView dashboard(SessionUser actor) {
        Policy.require(actor, STATS_READ);
        return new StatsView(
                userDao.findByRole(Role.CLIENT).size(),
                contractDao.countByStatus(),
                claimDao.countByStatus(),
                jdbcDao.activePremiums(),
                jdbcDao.paymentsReceived(),
                jdbcDao.indemnitiesPaid(),
                jdbcDao.revenueByMonth());
    }
}
