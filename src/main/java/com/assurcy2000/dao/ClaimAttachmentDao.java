package com.assurcy2000.dao;

import com.assurcy2000.model.ClaimAttachment;

import java.util.List;

public class ClaimAttachmentDao extends BaseDao<ClaimAttachment, Long> {

    public ClaimAttachmentDao() { super(ClaimAttachment.class); }

    public List<ClaimAttachment> findByClaim(Long claimId) {
        return query(s -> s.createQuery(
                "select a from ClaimAttachment a where a.claim.id = :claimId order by a.addedAt, a.id", ClaimAttachment.class)
                .setParameter("claimId", claimId)
                .getResultList());
    }

    public long countByClaim(Long claimId) {
        return query(s -> s.createQuery(
                "select count(a) from ClaimAttachment a where a.claim.id = :claimId", Long.class)
                .setParameter("claimId", claimId)
                .getSingleResult());
    }
}
