package com.assurcy2000.security;

import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;

import java.io.Serializable;

public record SessionUser(Long id, String email, String fullName, String role) implements Serializable {

    public static SessionUser from(User u) {
        return new SessionUser(u.getId(), u.getEmail(),
                u.getFirstName() + " " + u.getLastName(), u.getRole().getName());
    }

    public boolean is(String roleName) { return role.equals(roleName); }

    public String homePath() {
        return switch (role) {
            case Role.ADMIN -> "/admin/users";
            case Role.AGENT -> "/agent/contracts";
            default -> "/client/contracts";
        };
    }
}
