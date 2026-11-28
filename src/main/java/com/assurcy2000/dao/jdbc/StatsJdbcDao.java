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

    public long newClients() {
        return scalar("SELECT COUNT(*) FROM utilisateur u JOIN role r ON r.id = u.role_id "
                + "WHERE r.nom = 'CLIENT' AND u.date_creation >= DATE_SUB(NOW(), INTERVAL 30 DAY)").longValue();
    }

    public BigDecimal averageProcessingDays() {
        BigDecimal v = scalar("SELECT AVG(GREATEST(0, DATEDIFF(date_indemnisation, DATE(date_declaration)))) "
                + "FROM sinistre WHERE statut = 'INDEMNISE' AND date_indemnisation IS NOT NULL");
        return v;
    }

    public Map<String, Long> clientsByType() {
        return map("SELECT t.nom, COUNT(DISTINCT c.client_id) FROM type_assurance t "
                + "LEFT JOIN contrat c ON c.type_assurance_id = t.id AND c.statut IN ('ACTIF', 'EN_ATTENTE', 'SUSPENDU') "
                + "GROUP BY t.id, t.nom ORDER BY t.nom", rs -> rs.getLong(2));
    }

    public Map<String, BigDecimal> revenueByMonth() {
        return map("SELECT DATE_FORMAT(date_paiement, '%Y-%m') AS mois, SUM(montant) "
                + "FROM paiement WHERE statut = 'EFFECTUE' AND date_paiement IS NOT NULL "
                + "GROUP BY mois ORDER BY mois", rs -> rs.getBigDecimal(2));
    }

    public Map<String, BigDecimal> indemnitiesByMonth() {
        return map("SELECT DATE_FORMAT(date_indemnisation, '%Y-%m') AS mois, SUM(montant_indemnise) "
                + "FROM sinistre WHERE statut = 'INDEMNISE' AND date_indemnisation IS NOT NULL "
                + "GROUP BY mois ORDER BY mois", rs -> rs.getBigDecimal(2));
    }

    public Map<String, Long> contractsByMonth() {
        return map("SELECT DATE_FORMAT(date_debut, '%Y-%m') AS mois, COUNT(*) "
                + "FROM contrat WHERE statut <> 'EN_ATTENTE' GROUP BY mois ORDER BY mois", rs -> rs.getLong(2));
    }

    @FunctionalInterface
    private interface ValueReader<V> { V read(ResultSet rs) throws java.sql.SQLException; }

    private <V> Map<String, V> map(String sql, ValueReader<V> value) {
        try (var session = HibernateUtil.getSessionFactory().openSession()) {
            return session.doReturningWork(conn -> {
                Map<String, V> result = new LinkedHashMap<>();
                try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                    while (rs.next()) result.put(rs.getString(1), value.read(rs));
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