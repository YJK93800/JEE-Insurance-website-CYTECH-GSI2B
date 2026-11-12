package assurcy2000.dto;

import assurcy2000.model.User;

import java.io.Serializable;

/**
 * Ce qu'on garde en session après la connexion (attribut "utilisateur").
 * On ne met PAS l'entité User en session : elle contient le hash du mot de passe
 * et elle est liée à Hibernate. Ici, seulement ce dont les JSP et les filtres ont besoin.
 *
 * Noms des getters = noms utilisés dans les JSP : ${sessionScope.utilisateur.prenom}, .nom, .role
 */
public class SessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final Long id;
    private final String prenom;
    private final String nom;
    private final String email;
    private final String role; // "CLIENT", "AGENT" ou "ADMIN"

    public SessionUser(Long id, String prenom, String nom, String email, String role) {
        this.id = id;
        this.prenom = prenom;
        this.nom = nom;
        this.email = email;
        this.role = role;
    }

    public static SessionUser from(User user) {
        return new SessionUser(user.getId(), user.getFirstName(), user.getName(),
                user.getEmail(), user.getRole().getName());
    }

    public Long getId() { return id; }
    public String getPrenom() { return prenom; }
    public String getNom() { return nom; }
    public String getEmail() { return email; }
    public String getRole() { return role; }

    public boolean hasRole(String roleName) { return role.equals(roleName); }

    /** Page d'accueil de l'espace correspondant au rôle (même logique que index.jsp). */
    public String homePath() {
        return switch (role) {
            case "ADMIN" -> "/admin/stats";
            case "AGENT" -> "/agent/sinistres";
            default -> "/client/dashboard";
        };
    }
}
