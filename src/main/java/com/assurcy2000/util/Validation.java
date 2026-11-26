package com.assurcy2000.util;

import java.math.BigDecimal;
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
    public static final int GUARANTEE_NAME_MAX = 100; // garantie.nom VARCHAR(100)
    public static final int DESCRIPTION_MAX = 500;    // description TEXT, limitée dans les formulaires

    /** Même expression que l'attribut pattern des champs téléphone des JSP (20 caractères max = VARCHAR(20)). */
    public static final String PHONE_PATTERN = "[+0-9][0-9 .]{5,19}";

    private static final Pattern PHONE = Pattern.compile(PHONE_PATTERN);
    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");

    private Validation() {} // classe utilitaire : pas d'instance

    public static String firstName(String value, Map<String, String> errors) {
        return requiredText(value, "firstName", "Le prénom", NAME_MAX, errors);
    }

    public static String lastName(String value, Map<String, String> errors) {
        return requiredText(value, "lastName", "Le nom", NAME_MAX, errors);
    }

    /** Nom d'une garantie, obligatoire (champ "name"). */
    public static String guaranteeName(String value, Map<String, String> errors) {
        return requiredText(value, "name", "Le nom", GUARANTEE_NAME_MAX, errors);
    }

    /** Nom d'un type d'assurance, obligatoire (champ "name", type_assurance.nom VARCHAR(100)). */
    public static String insuranceTypeName(String value, Map<String, String> errors) {
        return requiredText(value, "name", "Le nom", GUARANTEE_NAME_MAX, errors);
    }

    /** Description facultative (champ "description") : renvoie null si vide. */
    public static String description(String value, Map<String, String> errors) {
        String v = Strings.trimToEmpty(value);
        if (v.isEmpty()) return null;
        if (v.length() > DESCRIPTION_MAX) errors.put("description", "La description est trop longue (" + DESCRIPTION_MAX + " caractères max).");
        return v;
    }

    /** Email obligatoire, renvoyé en minuscules. */
    public static String email(String value, Map<String, String> errors) {
        String v = Strings.trimToEmpty(value).toLowerCase();
        if (v.isEmpty()) errors.put("email", "L'email est obligatoire.");
        else if (v.length() > EMAIL_MAX || !EMAIL.matcher(v).matches()) errors.put("email", "L'email n'est pas valide.");
        return v;
    }

    /** Téléphone facultatif : renvoie null si vide. */
    public static String phone(String value, Map<String, String> errors) {
        String v = Strings.trimToEmpty(value);
        if (v.isEmpty()) return null;
        if (!PHONE.matcher(v).matches()) errors.put("phone", "Le numéro de téléphone n'est pas valide.");
        return v;
    }

    /** Adresse facultative : renvoie null si vide. */
    public static String address(String value, Map<String, String> errors) {
        String v = Strings.trimToEmpty(value);
        if (v.isEmpty()) return null;
        if (v.length() > ADDRESS_MAX) errors.put("address", "L'adresse est trop longue (" + ADDRESS_MAX + " caractères max).");
        return v;
    }

    /** Date de naissance obligatoire (format AAAA-MM-JJ du champ date), au moins 18 ans. Renvoie null si invalide. */
    public static LocalDate birthDate(String value, Map<String, String> errors) {
        String v = Strings.trimToEmpty(value);
        if (v.isEmpty()) {
            errors.put("birthDate", "La date de naissance est obligatoire.");
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(v);
            if (date.isAfter(LocalDate.now().minusYears(MIN_AGE))) {
                errors.put("birthDate", "Vous devez avoir au moins " + MIN_AGE + " ans.");
            } else if (date.isBefore(LocalDate.now().minusYears(120))) {
                errors.put("birthDate", "La date de naissance n'est pas valide.");
            }
            return date;
        } catch (DateTimeParseException e) {
            errors.put("birthDate", "La date de naissance n'est pas valide.");
            return null;
        }
    }

    /**
     * Montant en euros saisi dans un formulaire : accepte la virgule ou le point ("12,50" ou "12.50").
     * Doit être strictement positif, au plus égal à max, avec 2 décimales au maximum.
     * Champ vide : erreur si required, sinon null (ex. prime vide = prix de base du type).
     * Renvoie null si le montant est absent ou invalide.
     *
     * @param label début du message d'erreur, ex. "Le montant", "La prime"
     */
    public static BigDecimal amount(String value, String field, String label, BigDecimal max,
                                    boolean required, Map<String, String> errors) {
        String v = Strings.trimToEmpty(value).replace(',', '.');
        if (v.isEmpty()) {
            if (required) errors.put(field, label + " est obligatoire.");
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(v);
            if (amount.signum() <= 0 || amount.scale() > 2) {
                errors.put(field, label + " doit être un montant positif (2 décimales max).");
                return null;
            }
            if (amount.compareTo(max) > 0) {
                errors.put(field, label + " ne peut pas dépasser " + max.toPlainString() + " €.");
                return null;
            }
            return amount;
        } catch (NumberFormatException e) {
            errors.put(field, label + " n'est pas valide.");
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
        String v = Strings.trimToEmpty(value);
        if (v.isEmpty()) errors.put(field, label + " est obligatoire.");
        else if (v.length() > max) errors.put(field, label + " est trop long (" + max + " caractères max).");
        return v;
    }
}
