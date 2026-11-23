package com.assurcy2000.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Règles de validation des saisies, appelées par les servlets avant tout enregistrement.
 * Chaque méthode ajoute son erreur dans la map (clé = nom du champ dans la JSP)
 * et renvoie la valeur nettoyée.
 *
 * Les limites correspondent aux colonnes de la table utilisateur (V1__init_schema.sql)
 * et aux attributs maxlength / pattern des JSP : garder les trois en accord.
 */
public final class Validation {

    public static final int NAME_MAX = 80;       // utilisateur.nom / prenom VARCHAR(80)
    public static final int EMAIL_MAX = 150;     // utilisateur.email VARCHAR(150)
    public static final int ADDRESS_MAX = 255;   // utilisateur.adresse VARCHAR(255)
    public static final int MIN_AGE = 18;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 72;   // limite de BCrypt

    /** Même expression que l'attribut pattern des champs téléphone des JSP (20 caractères max = VARCHAR(20)). */
    public static final String PHONE_PATTERN = "[+0-9][0-9 .]{5,19}";

    private static final Pattern PHONE = Pattern.compile(PHONE_PATTERN);
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private Validation() {} // classe utilitaire : pas d'instance

    public static String firstName(String value, Map<String, String> errors) {
        return requiredText(value, "prenom", "Le prénom", NAME_MAX, errors);
    }

    public static String lastName(String value, Map<String, String> errors) {
        return requiredText(value, "nom", "Le nom", NAME_MAX, errors);
    }

    /** Email obligatoire, renvoyé en minuscules. */
    public static String email(String value, Map<String, String> errors) {
        String v = trim(value).toLowerCase();
        if (v.isEmpty()) errors.put("email", "L'email est obligatoire.");
        else if (v.length() > EMAIL_MAX || !EMAIL.matcher(v).matches()) errors.put("email", "L'email n'est pas valide.");
        return v;
    }

    /** Téléphone facultatif : renvoie null si vide. */
    public static String phone(String value, Map<String, String> errors) {
        String v = trim(value);
        if (v.isEmpty()) return null;
        if (!PHONE.matcher(v).matches()) errors.put("telephone", "Le numéro de téléphone n'est pas valide.");
        return v;
    }

    /** Adresse facultative : renvoie null si vide. */
    public static String address(String value, Map<String, String> errors) {
        String v = trim(value);
        if (v.isEmpty()) return null;
        if (v.length() > ADDRESS_MAX) errors.put("adresse", "L'adresse est trop longue (" + ADDRESS_MAX + " caractères max).");
        return v;
    }

    /** Date de naissance obligatoire (format AAAA-MM-JJ du champ date), au moins 18 ans. Renvoie null si invalide. */
    public static LocalDate birthDate(String value, Map<String, String> errors) {
        String v = trim(value);
        if (v.isEmpty()) {
            errors.put("dateNaissance", "La date de naissance est obligatoire.");
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(v);
            if (date.isAfter(LocalDate.now().minusYears(MIN_AGE))) {
                errors.put("dateNaissance", "Vous devez avoir au moins " + MIN_AGE + " ans.");
            } else if (date.isBefore(LocalDate.now().minusYears(120))) {
                errors.put("dateNaissance", "La date de naissance n'est pas valide.");
            }
            return date;
        } catch (DateTimeParseException e) {
            errors.put("dateNaissance", "La date de naissance n'est pas valide.");
            return null;
        }
    }

    /**
     * Vérifie que le mot de passe est valide
     * Ajoute les erreurs dans la map, sous les noms de champs donnés.
     */
    public static void validatePassword(String password, String confirmation,
                                        String passwordField, String confirmationField,
                                        Map<String, String> errors) {
        if (password == null) password = "";
        if (password.length() < PASSWORD_MIN || !password.matches(".*\\d.*"))
            errors.put(passwordField, PASSWORD_MIN + " caractères minimum, dont un chiffre.");
        else if (password.length() > PASSWORD_MAX)
            errors.put(passwordField, PASSWORD_MAX + " caractères maximum.");

        if (!password.equals(confirmation))
            errors.put(confirmationField, "Les mots de passe ne correspondent pas.");
    }

    private static String requiredText(String value, String field, String label, int max, Map<String, String> errors) {
        String v = trim(value);
        if (v.isEmpty()) errors.put(field, label + " est obligatoire.");
        else if (v.length() > max) errors.put(field, label + " est trop long (" + max + " caractères max).");
        return v;
    }

    private static String trim(String s) { return s == null ? "" : s.trim(); }
}
