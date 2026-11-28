package com.assurcy2000.util;

import com.assurcy2000.dto.request.UploadedFile;
import com.assurcy2000.model.enums.ClaimType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
    public static final int CLAIM_DESCRIPTION_MIN = 10;
    public static final int LONG_TEXT_MAX = 2000;     // description d'un sinistre, commentaire d'agent
    public static final BigDecimal CLAIM_AMOUNT_MAX = new BigDecimal("10000000"); // montant estimé et indemnisation

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

    /** Identifiant choisi dans une liste (contrat…) : obligatoire et numérique. Renvoie null si invalide. */
    public static Long requiredId(String value, String field, String message, Map<String, String> errors) {
        try {
            return Long.valueOf(Strings.trimToEmpty(value));
        } catch (NumberFormatException e) {
            errors.put(field, message);
            return null;
        }
    }

    // ---------------------------------------------------------------- sinistres

    /** Date du sinistre (champ "claimDate") : obligatoire, format AAAA-MM-JJ, pas dans le futur. Null si invalide. */
    public static LocalDate claimDate(String value, Map<String, String> errors) {
        String v = Strings.trimToEmpty(value);
        if (v.isEmpty()) {
            errors.put("claimDate", "La date du sinistre est obligatoire.");
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(v);
            if (date.isAfter(LocalDate.now())) {
                errors.put("claimDate", "La date du sinistre ne peut pas être dans le futur.");
            }
            return date;
        } catch (DateTimeParseException e) {
            errors.put("claimDate", "La date n'est pas valide.");
            return null;
        }
    }

    /** Type de sinistre (champ "type") : une valeur de ClaimType. Null si invalide. */
    public static ClaimType claimType(String value, Map<String, String> errors) {
        try {
            return ClaimType.valueOf(Strings.trimToEmpty(value));
        } catch (IllegalArgumentException e) {
            errors.put("type", "Type de sinistre invalide.");
            return null;
        }
    }

    /** Description d'un sinistre (champ "description") : 10 à 2000 caractères. */
    public static String claimDescription(String value, Map<String, String> errors) {
        String v = Strings.trimToEmpty(value);
        if (v.length() < CLAIM_DESCRIPTION_MIN) {
            errors.put("description", "Décrivez le sinistre (" + CLAIM_DESCRIPTION_MIN + " caractères minimum).");
        } else if (v.length() > LONG_TEXT_MAX) {
            errors.put("description", "Description trop longue (" + LONG_TEXT_MAX + " caractères max).");
        }
        return v;
    }

    /** Commentaire d'un agent sur un sinistre (champ "content") : 2 à 2000 caractères. */
    public static String comment(String value, Map<String, String> errors) {
        String v = Strings.trimToEmpty(value);
        if (v.length() < 2) errors.put("content", "Écrivez un commentaire.");
        else if (v.length() > LONG_TEXT_MAX) errors.put("content", "Commentaire trop long (" + LONG_TEXT_MAX + " caractères max).");
        return v;
    }

    /**
     * Pièces justificatives (champ "attachments") : nombre (avec celles déjà jointes), taille, et type réel
     * (PDF, JPEG, PNG reconnus à leurs premiers octets). S'arrête à la première erreur.
     * Renvoie le type de chaque fichier, dans le même ordre (liste incomplète en cas d'erreur).
     */
    public static List<AttachmentStorage.Kind> attachments(List<UploadedFile> files, long alreadyAttached,
                                                           Map<String, String> errors) {
        List<AttachmentStorage.Kind> kinds = new ArrayList<>();
        if (alreadyAttached + files.size() > AttachmentStorage.MAX_FILES) {
            errors.put("attachments", AttachmentStorage.MAX_FILES + " pièces justificatives maximum par sinistre"
                    + (alreadyAttached > 0 ? " (" + alreadyAttached + " déjà jointe(s))." : "."));
            return kinds;
        }
        for (UploadedFile file : files) {
            String name = fileName(file.name());
            if (file.size() <= 0) {
                errors.put("attachments", "« " + name + " » est vide.");
                break;
            }
            if (file.size() > AttachmentStorage.MAX_SIZE) {
                errors.put("attachments", "« " + name + " » dépasse 5 Mo.");
                break;
            }
            Optional<AttachmentStorage.Kind> kind = AttachmentStorage.detect(file);
            if (kind.isEmpty()) {
                errors.put("attachments", "« " + name + " » n'est pas un PDF, un JPG ou un PNG.");
                break;
            }
            kinds.add(kind.get());
        }
        return kinds;
    }

    /** Nom de fichier affichable : sans chemin (certains navigateurs l'envoient), sans caractères de contrôle, 255 max. */
    public static String fileName(String raw) {
        String name = raw == null ? "" : raw;
        name = name.substring(Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\')) + 1);
        name = name.replaceAll("\\p{Cntrl}", "").trim();
        if (name.isEmpty()) name = "fichier";
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
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
