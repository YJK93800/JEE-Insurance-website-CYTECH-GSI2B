package com.assurcy2000.security;

import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoleFilterTest {

    private static SessionUser as(String role) { return new SessionUser(1L, "X", "Y", "x@x.fr", role); }

    @Test
    void eachSpaceIsReservedToItsRoles() {
        assertTrue(RoleFilter.isAllowed("/client/contracts", as(Role.CLIENT)));
        assertFalse(RoleFilter.isAllowed("/client/contracts", as(Role.AGENT)));

        assertTrue(RoleFilter.isAllowed("/agent/claims", as(Role.AGENT)));
        assertTrue(RoleFilter.isAllowed("/agent/claims", as(Role.ADMIN)));
        assertFalse(RoleFilter.isAllowed("/agent/claims", as(Role.CLIENT)));

        assertTrue(RoleFilter.isAllowed("/admin/users", as(Role.ADMIN)));
        assertFalse(RoleFilter.isAllowed("/admin/users", as(Role.AGENT)));
    }

    @Test
    void prefixWithoutSlashAndQueryStringAreHandled() {
        assertFalse(RoleFilter.isAllowed("/admin", as(Role.CLIENT)));
        assertFalse(RoleFilter.isAllowed("/admin/users?id=3", as(Role.CLIENT)));
        assertTrue(RoleFilter.isAllowed("/client/claims?id=93", as(Role.CLIENT)));
    }

    @Test
    void otherPagesDoNotDependOnRole() {
        assertTrue(RoleFilter.isAllowed("/profil", as(Role.CLIENT)));
        assertTrue(RoleFilter.isAllowed("/", as(Role.AGENT)));
        assertTrue(RoleFilter.isAllowed("/administration", as(Role.CLIENT))); // pas le préfixe "/admin/"
    }

    @Test
    void anonymousUserIsRefusedInRoleSpaces() {
        assertFalse(RoleFilter.isAllowed("/client/contracts", null));
        assertTrue(RoleFilter.isAllowed("/login", null));
    }
}
