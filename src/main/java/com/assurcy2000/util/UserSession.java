package com.assurcy2000.util;

import com.assurcy2000.dto.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Tout ce qui touche à la session HTTP, au même endroit.
 * Les servlets et les filtres n'écrivent jamais "utilisateur" ou "csrfToken" à la main :
 * ils passent par ces méthodes. Si un nom d'attribut change, on ne le change qu'ici.
 */
public final class UserSession {

    // Noms des attributs de session (utilisés aussi dans les JSP : sessionScope.utilisateur...)
    public static final String USER = "utilisateur";
    public static final String CSRF_TOKEN = "csrfToken";
    public static final String REDIRECT_AFTER_LOGIN = "redirectAfterLogin";
    public static final String FLASH = "flash";
    public static final String FLASH_TYPE = "flashType";

    private static final int TIMEOUT_SECONDS = 30 * 60; // déconnexion après 30 min d'inactivité
    private static final SecureRandom RANDOM = new SecureRandom();

    private UserSession() {}

    /** Utilisateur connecté, ou null. Ne crée pas de session s'il n'y en a pas. */
    public static SessionUser currentUser(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        return session == null ? null : (SessionUser) session.getAttribute(USER);
    }

    public static boolean isLoggedIn(HttpServletRequest req) {
        return currentUser(req) != null;
    }

    /**
     * Ouvre la session après une connexion réussie.
     * @return la page mémorisée avant la connexion (ou null), qui est retirée de la session
     */
    public static String login(HttpServletRequest req, SessionUser user) {
        HttpSession session = req.getSession();
        String redirect = (String) session.getAttribute(REDIRECT_AFTER_LOGIN);
        session.removeAttribute(REDIRECT_AFTER_LOGIN);

        req.changeSessionId(); // nouvel identifiant : protège contre la fixation de session
        session.setAttribute(USER, user);
        session.setAttribute(CSRF_TOKEN, newToken()); // lu par csrf.jspf
        session.setMaxInactiveInterval(TIMEOUT_SECONDS);
        return redirect;
    }

    /** Ferme la session (supprime tous les attributs d'un coup). */
    public static void logout(HttpServletRequest req) {
        HttpSession session = req.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** Mémorise la page demandée par un visiteur non connecté, pour y revenir après la connexion. */
    public static void rememberRedirect(HttpServletRequest req, String path) {
        req.getSession().setAttribute(REDIRECT_AFTER_LOGIN, path);
    }

    /** Message affiché une seule fois sur la page suivante (par flash.jspf). type = "success", "error"... */
    public static void flash(HttpServletRequest req, String message, String type) {
        HttpSession session = req.getSession(); // crée une session si besoin (ex. juste après logout)
        session.setAttribute(FLASH, message);
        session.setAttribute(FLASH_TYPE, type);
    }

    private static String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
