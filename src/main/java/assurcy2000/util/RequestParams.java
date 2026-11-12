package assurcy2000.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestParams {
    private RequestParams() {}

    /** Paramètre nettoyé des espaces, "" si absent. */
    public static String get(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return value == null ? "" : value.trim();
    }

    /** Paramètre brut (sans trim), "" si absent. À utiliser pour les mots de passe. */
    public static String raw(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return value == null ? "" : value;
    }
}
