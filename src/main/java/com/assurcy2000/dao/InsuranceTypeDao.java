package com.assurcy2000.dao;

import com.assurcy2000.model.Guarantee;
import com.assurcy2000.model.InsuranceType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class InsuranceTypeDao extends BaseDao<InsuranceType, Long> {

    public InsuranceTypeDao() { super(InsuranceType.class); }

    @Override
    public Optional<InsuranceType> findById(Long id) {
        return query(s -> s.createQuery(
                "select t from InsuranceType t left join fetch t.guarantees where t.id = :id",
                InsuranceType.class)
                .setParameter("id", id)
                .uniqueResultOptional());
    }

    @Override
    public List<InsuranceType> findAll() {
        return query(s -> s.createQuery(
                "select t from InsuranceType t left join fetch t.guarantees order by t.name",
                InsuranceType.class)
                .getResultList());
    }

    public List<InsuranceType> findAllActive() {
        return query(s -> s.createQuery(
                "select t from InsuranceType t left join fetch t.guarantees where t.active = true order by t.name",
                InsuranceType.class)
                .getResultList());
    }

    public Optional<InsuranceType> findByName(String name) {
        return query(s -> s.createQuery("from InsuranceType t where t.name = :name", InsuranceType.class)
                .setParameter("name", name)
                .uniqueResultOptional());
    }

    /** Nombre de contrats par type, en une requête : id du type -> nombre (absent = 0). */
    public Map<Long, Long> countContractsByType() {
        return query(s -> {
            Map<Long, Long> result = new HashMap<>();
            s.createQuery("select c.insuranceType.id, count(c) from Contract c group by c.insuranceType.id", Object[].class)
                    .getResultList()
                    .forEach(row -> result.put((Long) row[0], (Long) row[1]));
            return result;
        });
    }

    public boolean isUsedByContract(Long insuranceTypeId) {
        return query(s -> s.createQuery(
                "select count(c) from Contract c where c.insuranceType.id = :id", Long.class)
                .setParameter("id", insuranceTypeId)
                .getSingleResult() > 0);
    }

    public void addGuarantee(Long insuranceTypeId, Long guaranteeId) {
        changeLink(insuranceTypeId, Guarantee.class, guaranteeId, InsuranceType::getGuarantees, true);
    }

    public void removeGuarantee(Long insuranceTypeId, Long guaranteeId) {
        changeLink(insuranceTypeId, Guarantee.class, guaranteeId, InsuranceType::getGuarantees, false);
    }
}
