package assurcy2000.service;

/**
 * Erreur métier lors de l'inscription (email ou téléphone déjà utilisé...).
 * "field" = nom du champ du formulaire concerné, pour afficher l'erreur au bon endroit.
 */
public class RegistrationException extends RuntimeException {

    private final String field;

    public RegistrationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
