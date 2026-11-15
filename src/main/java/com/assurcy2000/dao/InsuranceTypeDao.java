package com.assurcy2000.dao;

import com.assurcy2000.model.Guarantee;
import com.assurcy2000.model.InsuranceType;

import java.util.List;
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

    public void addGuarantee(Long insuranceTypeId, Long guaranteeId) {
        runInTransaction(s -> {
            InsuranceType type = s.find(InsuranceType.class, insuranceTypeId);
            Guarantee guarantee = s.find(Guarantee.class, guaranteeId);
            if (type == null || guarantee == null) {
                throw new IllegalArgumentException("Type d'assurance ou garantie introuvable");
            }
            type.getGuarantees().add(guarantee);
        });
    }

    public void removeGuarantee(Long insuranceTypeId, Long guaranteeId) {
        runInTransaction(s -> {
            InsuranceType type = s.find(InsuranceType.class, insuranceTypeId);
            Guarantee guarantee = s.find(Guarantee.class, guaranteeId);
            if (type == null || guarantee == null) {
                throw new IllegalArgumentException("Type d'assurance ou garantie introuvable");
            }
            type.getGuarantees().remove(guarantee);
        });
    }
}
