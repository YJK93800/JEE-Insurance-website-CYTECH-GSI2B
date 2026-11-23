package com.assurcy2000.security;

import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Role;
import com.assurcy2000.security.UserSession;
import jakarta.servlet.*;
import jakarta.servlet.http.*;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Contrôle d'accès par rôle, selon le préfixe de l'URL.
 * RULES est la seule définition de « quel rôle a accès à quel espace » :
 * le filtre l'applique à chaque requête et LoginServlet l'utilise (via isAllowed)
 * pour vérifier la page mémorisée avant d'y rediriger après la connexion.
 */
public class RoleFilter implements Filter {

    private static final Map<String, Set<String>> RULES = Map.of(
            "/client/", Set.of(Role.CLIENT),
            "/agent/", Set.of(Role.AGENT, Role.ADMIN),
            "/admin/", Set.of(Role.ADMIN));

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        if (!isAllowed(UserSession.currentPath(req), UserSession.currentUser(req))) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        chain.doFilter(request, response);
    }

    /**
     * La page est-elle accessible pour cet utilisateur ?
     * path : chemin dans l'application, avec ou sans query string ("/admin/users?id=3").
     * Les pages hors /client/, /agent/ et /admin/ ne dépendent pas du rôle.
     */
    public static boolean isAllowed(String path, SessionUser user) {
        int query = path.indexOf('?');
        // "/" final : "/admin" doit être traité comme "/admin/"
        String normalized = (query >= 0 ? path.substring(0, query) : path) + "/";
        for (var rule : RULES.entrySet()) {
            if (normalized.startsWith(rule.getKey())) {
                return user != null && rule.getValue().contains(user.role());
            }
        }
        return true;
    }
}
