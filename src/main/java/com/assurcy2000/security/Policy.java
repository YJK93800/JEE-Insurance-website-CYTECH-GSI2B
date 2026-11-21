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
}
