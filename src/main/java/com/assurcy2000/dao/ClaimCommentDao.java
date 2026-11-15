package com.assurcy2000.dao;

import com.assurcy2000.model.ClaimComment;

import java.util.List;

public class ClaimCommentDao extends BaseDao<ClaimComment, Long> {

    public ClaimCommentDao() { super(ClaimComment.class); }

    public List<ClaimComment> findByClaim(Long claimId) {
        return query(s -> s.createQuery(
                "select m from ClaimComment m join fetch m.author where m.claim.id = :claimId order by m.createdAt, m.id",
                ClaimComment.class)
                .setParameter("claimId", claimId)
                .getResultList());
    }
}
