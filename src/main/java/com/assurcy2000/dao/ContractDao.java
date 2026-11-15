package com.assurcy2000.dao;

import com.assurcy2000.model.Contract;
import com.assurcy2000.model.Guarantee;
import com.assurcy2000.model.enums.ContractStatus;

import java.time.LocalDate;
import java.util.EnumMap;
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

    public List<Contract> search(String keyword) {
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        return query(s -> s.createQuery(SELECT
                + " where lower(c.number) like :p or lower(cl.lastName) like :p"
                + " or lower(cl.firstName) like :p or lower(cl.email) like :p"
                + " order by c.startDate desc", Contract.class)
                .setParameter("p", pattern)
                .getResultList());
    }

    public List<Contract> findActiveEndedBefore(LocalDate date) {
        return query(s -> s.createQuery(SELECT + " where c.status = :status and c.endDate < :date", Contract.class)
                .setParameter("status", ContractStatus.ACTIF)
                .setParameter("date", date)
                .getResultList());
    }

    public Map<ContractStatus, Long> countByStatus() {
        return query(s -> {
            Map<ContractStatus, Long> result = new EnumMap<>(ContractStatus.class);
            for (ContractStatus status : ContractStatus.values()) {
                result.put(status, 0L);
            }
            s.createQuery("select c.status, count(c) from Contract c group by c.status", Object[].class)
                    .getResultList()
                    .forEach(row -> result.put((ContractStatus) row[0], (Long) row[1]));
            return result;
        });
    }

    public void addGuarantee(Long contractId, Long guaranteeId) {
        runInTransaction(s -> {
            Contract contract = s.find(Contract.class, contractId);
            Guarantee guarantee = s.find(Guarantee.class, guaranteeId);
            if (contract == null || guarantee == null) {
                throw new IllegalArgumentException("Contrat ou garantie introuvable");
            }
            contract.getGuarantees().add(guarantee);
        });
    }

    public void removeGuarantee(Long contractId, Long guaranteeId) {
        runInTransaction(s -> {
            Contract contract = s.find(Contract.class, contractId);
            Guarantee guarantee = s.find(Guarantee.class, guaranteeId);
            if (contract == null || guarantee == null) {
                throw new IllegalArgumentException("Contrat ou garantie introuvable");
            }
            contract.getGuarantees().remove(guarantee);
        });
    }
}
