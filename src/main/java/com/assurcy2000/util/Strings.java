package com.assurcy2000.util;

/** Petites fonctions de nettoyage des chaînes saisies. */
public final class Strings {

    private Strings() {} // classe utilitaire : pas d'instance

    /** null → "", sinon la chaîne sans espaces autour. */
    public static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /** null ou vide → null, sinon la chaîne sans espaces autour. */
    public static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
