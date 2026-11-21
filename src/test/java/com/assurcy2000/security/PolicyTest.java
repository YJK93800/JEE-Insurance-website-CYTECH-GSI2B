package com.assurcy2000.security;

import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Role;
import org.junit.jupiter.api.Test;

import static com.assurcy2000.security.Permission.*;
import static org.junit.jupiter.api.Assertions.*;

class PolicyTest {

    private static SessionUser as(String role) { return new SessionUser(1L, "X", "Y", "x@x.fr", role); }

    @Test
    void clientCannotReadEveryoneOrManageUsers() {
        SessionUser client = as(Role.CLIENT);
        assertTrue(Policy.can(client, CONTRACT_READ_OWN));
        assertFalse(Policy.can(client, CONTRACT_READ_ALL));
        assertFalse(Policy.can(client, CLIENT_READ));
        assertFalse(Policy.can(client, CLAIM_UPDATE));
        assertFalse(Policy.can(client, USER_MANAGE));
        assertFalse(Policy.can(client, STATS_READ));
    }

    @Test
    void agentCanManageContractsButNotUsers() {
        SessionUser agent = as(Role.AGENT);
        assertTrue(Policy.can(agent, CLIENT_READ));
        assertTrue(Policy.can(agent, CONTRACT_WRITE));
        assertTrue(Policy.can(agent, CLAIM_UPDATE));
        assertFalse(Policy.can(agent, USER_MANAGE));
    }

    @Test
    void adminManagesUsersButDoesNotEditContracts() {
        SessionUser admin = as(Role.ADMIN);
        assertTrue(Policy.can(admin, USER_MANAGE));
        assertTrue(Policy.can(admin, CONTRACT_READ_ALL));
        assertFalse(Policy.can(admin, CONTRACT_WRITE));
    }

    @Test
    void unknownRoleOrNoUserHasNothing() {
        assertFalse(Policy.can(as("PIRATE"), CONTRACT_READ_OWN));
        assertFalse(Policy.can(null, CONTRACT_READ_OWN));
    }
}
