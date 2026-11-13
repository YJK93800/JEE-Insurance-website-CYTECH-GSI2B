package com.assurcy2000.util;

import java.util.Map;

/** Règles de validation partagées entre servlets. */
public final class Validation {

    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 72; // limite de BCrypt

    private Validation() {} // classe utilitaire : pas d'instance

    /**
     * Vérifie que le mot de passe est valide
     * Ajoute les erreurs dans la map, sous les noms de champs donnés.
     */
    public static void validatePassword(String password, String confirmation,
                                        String passwordField, String confirmationField,
                                        Map<String, String> errors) {
        if (password.length() < PASSWORD_MIN || !password.matches(".*\\d.*"))
            errors.put(passwordField, PASSWORD_MIN + " caractères minimum, dont un chiffre.");
        else if (password.length() > PASSWORD_MAX)
            errors.put(passwordField, PASSWORD_MAX + " caractères maximum.");

        if (!password.equals(confirmation))
            errors.put(confirmationField, "Les mots de passe ne correspondent pas.");
    }
}
