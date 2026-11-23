package com.assurcy2000.util;

import jakarta.servlet.http.HttpServletRequest;

public final class RequestParams {
    private RequestParams() {}

    /** Paramètre nettoyé des espaces, "" si absent. */
    public static String get(HttpServletRequest req, String name) {
        return Strings.trimToEmpty(req.getParameter(name));
    }

    /** Paramètre brut (sans trim), "" si absent. À utiliser pour les mots de passe. */
    public static String raw(HttpServletRequest req, String name) {
        String value = req.getParameter(name);
        return value == null ? "" : value;
    }

    /** Paramètre converti en Long, null si absent ou invalide. */
    public static Long optionalLong(HttpServletRequest req, String name) {
        try {
            return Long.valueOf(get(req, name));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}