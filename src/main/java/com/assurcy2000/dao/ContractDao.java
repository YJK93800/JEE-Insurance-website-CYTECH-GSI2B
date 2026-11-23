package com.assurcy2000.dao;

import com.assurcy2000.dto.request.ContractSearchRequest;
import com.assurcy2000.model.Contract;
import com.assurcy2000.model.Guarantee;
import com.assurcy2000.model.enums.ContractStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ContractDao extends BaseDao<Contract, Long> {


    private static final String SELECT =
            "select c from Contract c join fetch c.client cl join fetch c.insuranceType t left join fetch c.agent a";

    public ContractDao() { super(Contract.class); }


    @Override
    public Optional<Contract> findById(Long id) {
        return query(s -> s.createQuery(SELECT + " left join fetch c.guarantees where c.id = :id", Contract.class)
                .setParameter("id", id)
                .uniqueResultOptional());
    }

    @Override
    public List<Contract> findAll() {
        return query(s -> s.createQuery(SELECT + " order by c.startDate desc", Contract.class)
                .getResultList());
    }

    public Optional<Contract> findByNumber(String number) {
        return query(s -> s.createQuery(SELECT + " where c.number = :number", Contract.class)
                .setParameter("number", number)
                .uniqueResultOptional());
    }

    /** Plus grand numéro commençant par le préfixe (ex. "CTR-2026-"), vide s'il n'y en a pas encore. */
    public Optional<String> findMaxNumberWithPrefix(String prefix) {
        return query(s -> Optional.ofNullable(s.createQuery(
                "select max(c.number) from Contract c where c.number like :p", String.class)
                .setParameter("p", prefix + "%")
                .getSingleResult()));
    }

    public boolean existsByNumber(String number) {
        return query(s -> s.createQuery(
                "select count(c) from Contract c where c.number = :number", Long.class)
                .setParameter("number", number)
                .getSingleResult() > 0);
    }

    public List<Contract> findByClient(Long clientId) {
        return query(s -> s.createQuery(SELECT + " where cl.id = :clientId order by c.startDate desc", Contract.class)
                .setParameter("clientId", clientId)
                .getResultList());
    }

    public List<Contract> findByStatus(ContractStatus status) {
        return query(s -> s.createQuery(SELECT + " where c.status = :status order by c.startDate desc", Contract.class)
                .setParameter("status", status)
                .getResultList());
    }

    /**
     * Recherche multi-critères : chaque critère null est ignoré.
     * Le mot-clé cherche dans le numéro de contrat et dans le nom, prénom et email du client.
     */
    public List<Contract> search(ContractSearchRequest criteria) {
        StringBuilder hql = new StringBuilder(SELECT).append(" where 1 = 1");
        String keyword = criteria.keyword() == null ? "" : criteria.keyword().trim().toLowerCase();
        if (!keyword.isEmpty()) {
            hql.append(" and (lower(c.number) like :p or lower(cl.lastName) like :p"
                    + " or lower(cl.firstName) like :p or lower(cl.email) like :p"
                    + " or lower(concat(cl.firstName, ' ', cl.lastName)) like :p)");
        }
        if (criteria.status() != null) hql.append(" and c.status = :status");
        if (criteria.insuranceTypeId() != null) hql.append(" and t.id = :typeId");
        hql.append(" order by c.startDate desc");

        return query(s -> {
            var q = s.createQuery(hql.toString(), Contract.class);
            if (!keyword.isEmpty()) q.setParameter("p", "%" + keyword + "%");
            if (criteria.status() != null) q.setParameter("status", criteria.status());
            if (criteria.insuranceTypeId() != null) q.setParameter("typeId", criteria.insuranceTypeId());
            return q.getResultList();
        });
    }

    public List<Contract> findActiveEndedBefore(LocalDate date) {
        return query(s -> s.createQuery(SELECT + " where c.status = :status and c.endDate < :date", Contract.class)
                .setParameter("status", ContractStatus.ACTIF)
                .setParameter("date", date)
                .getResultList());
    }

    public Map<ContractStatus, Long> countByStatus() {
        return countByEnum(ContractStatus.class, "status");
    }

    public void addGuarantee(Long contractId, Long guaranteeId) {
        changeLink(contractId, Guarantee.class, guaranteeId, Contract::getGuarantees, true);
    }

    public void removeGuarantee(Long contractId, Long guaranteeId) {
        changeLink(contractId, Guarantee.class, guaranteeId, Contract::getGuarantees, false);
    }
}
