package com.assurcy2000.security;

import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Role;
import com.assurcy2000.service.exceptions.ForbiddenException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.assurcy2000.security.Permission.*;

public final class Policy {

    private static final Map<String, Set<Permission>> BY_ROLE = Map.of(
            Role.CLIENT, EnumSet.of(
                    CONTRACT_READ_OWN, CLAIM_READ_OWN, CLAIM_CREATE_OWN, PAYMENT_READ_OWN, AI_ASSISTANT),
            Role.AGENT, EnumSet.of(
                    CLIENT_READ, CONTRACT_READ_ALL, CONTRACT_WRITE, CLAIM_READ_ALL, CLAIM_UPDATE,
                    PAYMENT_READ_ALL, PAYMENT_WRITE, GUARANTEE_MANAGE, STATS_READ),
            Role.ADMIN, EnumSet.of(
                    CLIENT_READ, CONTRACT_READ_ALL, CLAIM_READ_ALL, PAYMENT_READ_ALL,
                    INSURANCE_TYPE_MANAGE, STATS_READ, USER_MANAGE));

    private Policy() { }

    public static boolean can(SessionUser user, Permission permission) {
        return user != null && BY_ROLE.getOrDefault(user.role(), Set.of()).contains(permission);
    }

    public static void require(SessionUser user, Permission permission) {
        if (!can(user, permission)) throw new ForbiddenException();
    }

    /**
     * Pour ce qui est ouvert à tout utilisateur connecté, quel que soit son rôle
     * (son profil, le catalogue…). Renvoie l'utilisateur pour pouvoir l'utiliser directement.
     */
    public static SessionUser requireLoggedIn(SessionUser user) {
        if (user == null) throw new ForbiddenException();
        return user;
    }

    /*
     * Règle de lecture commune aux contrats, sinistres et paiements :
     *   permission "ALL" (agent, admin)  -> tout
     *   permission "OWN" (client)        -> seulement ce qui lui appartient
     *   aucune des deux                  -> interdit
     */

    /**
     * Pour une liste : true = l'utilisateur voit tout, false = seulement les siens.
     * Lève ForbiddenException s'il n'a ni l'une ni l'autre permission.
     */
    public static boolean readsAll(SessionUser user, Permission all, Permission own) {
        if (can(user, all)) return true;
        require(user, own);
        return false;
    }

    /**
     * Pour un élément précis : l'utilisateur peut-il le lire ?
     * ownerId = id du client propriétaire (ex. contract.getClient().getId()).
     * Les services renvoient « introuvable » quand c'est false, pour ne pas révéler qu'un id existe.
     */
    public static boolean canRead(SessionUser user, Permission all, Permission own, Long ownerId) {
        if (can(user, all)) return true;
        return can(user, own) && ownerId != null && ownerId.equals(user.id());
    }
}
