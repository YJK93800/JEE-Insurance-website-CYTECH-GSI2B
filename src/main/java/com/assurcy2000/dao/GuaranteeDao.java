package com.assurcy2000.dao;

import com.assurcy2000.model.Guarantee;

import java.util.List;
import java.util.Optional;

public class GuaranteeDao extends BaseDao<Guarantee, Long> {

    public GuaranteeDao() { super(Guarantee.class); }

    public Optional<Guarantee> findByName(String name) {
        return query(s -> s.createQuery("from Guarantee g where g.name = :name", Guarantee.class)
                .setParameter("name", name)
                .uniqueResultOptional());
    }

    public List<Guarantee> findAllActive() {
        return query(s -> s.createQuery(
                "from Guarantee g where g.active = true order by g.name", Guarantee.class)
                .getResultList());
    }

    public List<Guarantee> findByInsuranceType(Long insuranceTypeId) {
        return query(s -> s.createQuery(
                "select g from InsuranceType t join t.guarantees g where t.id = :id order by g.name",
                Guarantee.class)
                .setParameter("id", insuranceTypeId)
                .getResultList());
    }

    public List<Guarantee> findByContract(Long contractId) {
        return query(s -> s.createQuery(
                "select g from Contract c join c.guarantees g where c.id = :id order by g.name",
                Guarantee.class)
                .setParameter("id", contractId)
                .getResultList());
    }

    public boolean isUsedInContract(Long guaranteeId) {
        return query(s -> s.createQuery(
                "select count(c) from Contract c join c.guarantees g where g.id = :id", Long.class)
                .setParameter("id", guaranteeId)
                .getSingleResult() > 0);
    }
}
