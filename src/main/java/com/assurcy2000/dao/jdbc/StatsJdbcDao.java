package com.assurcy2000.dao.jdbc;

import com.assurcy2000.persistence.HibernateUtil;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

public class StatsJdbcDao {

    public BigDecimal activePremiums() {
        return scalar("SELECT COALESCE(SUM(prime), 0) FROM contrat WHERE statut = 'ACTIF'");
    }

    public BigDecimal paymentsReceived() {
        return scalar("SELECT COALESCE(SUM(montant), 0) FROM paiement WHERE statut = 'EFFECTUE'");
    }

    public BigDecimal indemnitiesPaid() {
        return scalar("SELECT COALESCE(SUM(montant_indemnise), 0) FROM sinistre WHERE statut = 'INDEMNISE'");
    }

    public Map<String, BigDecimal> revenueByMonth() {
        String sql = "SELECT DATE_FORMAT(date_paiement, '%Y-%m') AS mois, SUM(montant) "
                + "FROM paiement WHERE statut = 'EFFECTUE' AND date_paiement IS NOT NULL "
                + "GROUP BY mois ORDER BY mois";
        try (var session = HibernateUtil.getSessionFactory().openSession()) {
            return session.doReturningWork(conn -> {
                Map<String, BigDecimal> result = new LinkedHashMap<>();
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    while (rs.next()) result.put(rs.getString(1), rs.getBigDecimal(2));
                }
                return result;
            });
        }
    }

    private BigDecimal scalar(String sql) {
        try (var session = HibernateUtil.getSessionFactory().openSession()) {
            return session.doReturningWork(conn -> {
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    return rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO;
                }
            });
        }
    }
}
