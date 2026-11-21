package com.assurcy2000.dao;

import com.assurcy2000.model.AiMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AiMessageDao extends BaseDao<AiMessage, Long> {

    public AiMessageDao() { super(AiMessage.class); }

    public List<AiMessage> findByClient(Long clientId) {
        return query(s -> s.createQuery(
                "select m from AiMessage m where m.client.id = :clientId order by m.sentAt, m.id", AiMessage.class)
                .setParameter("clientId", clientId)
                .getResultList());
    }

    public List<AiMessage> findRecentByClient(Long clientId, int limit) {
        List<AiMessage> latest = query(s -> s.createQuery(
                "select m from AiMessage m where m.client.id = :clientId order by m.sentAt desc, m.id desc", AiMessage.class)
                .setParameter("clientId", clientId)
                .setMaxResults(limit)
                .getResultList());
        List<AiMessage> ordered = new ArrayList<>(latest);
        Collections.reverse(ordered);
        return ordered;
    }

    public int deleteByClient(Long clientId) {
        return query(s -> s.createMutationQuery("delete from AiMessage m where m.client.id = :clientId")
                .setParameter("clientId", clientId)
                .executeUpdate());
    }
}
