package com.assurcy2000.dao;

import com.assurcy2000.model.ClaimHistory;

import java.util.List;

public class ClaimHistoryDao extends BaseDao<ClaimHistory, Long> {

    public ClaimHistoryDao() { super(ClaimHistory.class); }

    public List<ClaimHistory> findByClaim(Long claimId) {
        return query(s -> s.createQuery(
                "select h from ClaimHistory h left join fetch h.changedBy where h.claim.id = :claimId order by h.changedAt, h.id",
                ClaimHistory.class)
                .setParameter("claimId", claimId)
                .getResultList());
    }
}
