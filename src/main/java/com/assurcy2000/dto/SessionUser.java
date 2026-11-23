package com.assurcy2000.dto;

import com.assurcy2000.model.Role;
import com.assurcy2000.model.User;

import java.io.Serializable;

/**
 * Ce qu'on garde en session après la connexion (attribut "utilisateur").
 * Pas l'entité User : elle contient le hash du mot de passe et elle est liée à Hibernate.
 * Dans les JSP : ${sessionScope.utilisateur.prenom}, .nom, .role
 */
public record SessionUser(Long id, String prenom, String nom, String email, String role)
        implements Serializable {

    public static SessionUser from(User u) {
        return new SessionUser(u.getId(), u.getFirstName(), u.getLastName(),
                u.getEmail(), u.getRole().getName());
    }

    public boolean is(String roleName) { return role.equals(roleName); }

    /** Page d'accueil de l'espace correspondant au rôle (même logique que index.jsp). */
    public String homePath() {
        return switch (role) {
            case Role.ADMIN -> "/admin/stats";
            case Role.AGENT -> "/agent/claims";
            default -> "/client/contracts"; // pas encore de servlet pour /client/dashboard
        };
    }
}