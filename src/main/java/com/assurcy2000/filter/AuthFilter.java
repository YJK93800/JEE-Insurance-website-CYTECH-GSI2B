package com.assurcy2000.filter;

import com.assurcy2000.dto.SessionUser;
import com.assurcy2000.model.Role;
import com.assurcy2000.util.UserSession;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Protège les espaces privés :
 *  - pas connecté            -> redirection vers /auth/login (puis retour à la page demandée)
 *  - connecté, mauvais rôle  -> erreur 403
 *
 * /client/* : CLIENT    /agent/* : AGENT    /admin/* : ADMIN
 *
 * ATTENTION : le filtre vérifie le RÔLE, pas la PROPRIÉTÉ des données.
 * Chaque servlet client doit vérifier que le contrat / sinistre demandé appartient bien
 * à sessionScope.utilisateur.id.
 */
@WebFilter(urlPatterns = {"/client/*", "/agent/*", "/admin/*", "/profil"})
public class AuthFilter extends HttpFilter {

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws IOException, ServletException {

        String path = req.getRequestURI().substring(req.getContextPath().length());
        SessionUser user = UserSession.currentUser(req);

        if (user == null) {
            // On ne mémorise que les GET : rejouer un POST après connexion n'a pas de sens
            if ("GET".equals(req.getMethod())) {
                String query = req.getQueryString();
                UserSession.rememberRedirect(req, query == null ? path : path + "?" + query);
            }
            resp.sendRedirect(req.getContextPath() + "/auth/login");
            return;
        }

        if (!isAllowed(path, user)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN); // affiche erreur.jsp (web.xml)
            return;
        }

        // Empêche le bouton "Précédent" d'afficher une page privée après la déconnexion
        resp.setHeader("Cache-Control", "no-store");
        chain.doFilter(req, resp);
    }

    /** Le rôle de l'utilisateur lui donne-t-il accès à ce chemin ? (utilisé aussi par LoginServlet) */
    public static boolean isAllowed(String path, SessionUser user) {
        if (path.startsWith("/client/")) return user.hasRole(Role.CLIENT);
        if (path.startsWith("/agent/"))  return user.hasRole(Role.AGENT);
        if (path.startsWith("/admin/"))  return user.hasRole(Role.ADMIN);
        return !path.startsWith("//"); // chemin interne au site uniquement
    }
}
