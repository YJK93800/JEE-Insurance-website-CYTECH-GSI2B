package com.assurcy2000.dao;

import com.assurcy2000.model.Claim;
import com.assurcy2000.model.enums.ClaimStatus;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ClaimDao extends BaseDao<Claim, Long> {

    private static final String SELECT =
            "select s from Claim s join fetch s.contract c join fetch c.client cl left join fetch s.agent a";

    public ClaimDao() { super(Claim.class); }

    @Override
    public Optional<Claim> findById(Long id) {
        return query(s -> s.createQuery(SELECT + " where s.id = :id", Claim.class)
                .setParameter("id", id)
                .uniqueResultOptional());
    }

    @Override
    public List<Claim> findAll() {
        return query(s -> s.createQuery(SELECT + " order by s.declaredAt desc", Claim.class)
                .getResultList());
    }

    public Optional<Claim> findByNumber(String number) {
        return query(s -> s.createQuery(SELECT + " where s.number = :number", Claim.class)
                .setParameter("number", number)
                .uniqueResultOptional());
    }

    public boolean existsByNumber(String number) {
        return query(s -> s.createQuery(
                "select count(s) from Claim s where s.number = :number", Long.class)
                .setParameter("number", number)
                .getSingleResult() > 0);
    }

    public List<Claim> findByClient(Long clientId) {
        return query(s -> s.createQuery(SELECT + " where cl.id = :clientId order by s.declaredAt desc", Claim.class)
                .setParameter("clientId", clientId)
                .getResultList());
    }

    public Optional<Claim> findLastByClient(Long clientId) {
        return query(s -> s.createQuery(SELECT + " where cl.id = :clientId order by s.declaredAt desc, s.id desc", Claim.class)
                .setParameter("clientId", clientId)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst());
    }

    public List<Claim> findByContract(Long contractId) {
        return query(s -> s.createQuery(SELECT + " where c.id = :contractId order by s.declaredAt desc", Claim.class)
                .setParameter("contractId", contractId)
                .getResultList());
    }

    public List<Claim> findByStatus(ClaimStatus status) {
        return query(s -> s.createQuery(SELECT + " where s.status = :status order by s.declaredAt", Claim.class)
                .setParameter("status", status)
                .getResultList());
    }

    public Map<ClaimStatus, Long> countByStatus() {
        return countByEnum(ClaimStatus.class, "status");
    }
}
