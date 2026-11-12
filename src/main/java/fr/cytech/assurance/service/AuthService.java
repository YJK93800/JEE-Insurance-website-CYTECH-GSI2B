package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.RoleDao;
import fr.cytech.assurance.dao.UserDao;
import fr.cytech.assurance.dao.impl.RoleDaoImpl;
import fr.cytech.assurance.dao.impl.UserDaoImpl;
import fr.cytech.assurance.dto.SessionUser;
import fr.cytech.assurance.dto.request.RegisterRequest;
import fr.cytech.assurance.model.User;
import fr.cytech.assurance.model.Role;
import fr.cytech.assurance.util.Tx;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Optional;

public class AuthService {

    /**
     * Hash BCrypt factice : quand l'email n'existe pas, on vérifie quand même un mot de passe
     * pour que la réponse prenne le même temps. Sinon on pourrait deviner quels emails existent.
     */
    private static final String DUMMY_HASH = BCrypt.hashpw("dummy-password", BCrypt.gensalt());

    private final UserDao users;
    private final RoleDao roles;

    public AuthService() {
        this(new UserDaoImpl(), new RoleDaoImpl());
    }

    // permet de passer d'autres DAO (utile pour les tests)
    public AuthService(UserDao users, RoleDao roles) {
        this.users = users;
        this.roles = roles;
    }

    /**
     * Crée un compte client. Les données doivent déjà avoir été validées par la servlet.
     * @param request : DTO qui envoie les champs qui ont été entrées dans l'inscription
     * @throws RegistrationException si l'email ou le téléphone est déjà utilisé
     */
    public User register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        String phone = request.getPhoneNumber();

        return Tx.call(() -> {
            if (users.findByEmail(email).isPresent()) {
                throw new RegistrationException("email", "Un compte existe déjà avec cette adresse e-mail.");
            }
            if (phone != null && users.findByPhoneNumber(phone).isPresent()) {
                throw new RegistrationException("phoneNumber", "Ce numéro de téléphone est déjà utilisé.");
            }

            User user = new User(
                    request.getFirstName(),
                    request.getName(),
                    email,
                    phone,
                    request.getBirthDate(),
                    request.getAddress(),
                    BCrypt.hashpw(request.getPassword(), BCrypt.gensalt())
            );

            Role client = roles.findByName(Role.CLIENT)
                    .orElseThrow(() -> new IllegalStateException("Rôle CLIENT absent de la table roles"));
            user.setRole(client);

            return users.save(user);
        });
    }

    /**
     * Vérifie l'email et le mot de passe.
     * @param email : Email utilisé pour se connecté.
     * @param password : Le mot de passe lié à l'email
     * @return l'utilisateur à mettre en session, ou vide si les identifiants sont faux.
     *         On ne dit jamais lequel des deux est faux.
     */
    public Optional<SessionUser> login(String email, String password) {
        String normalized = email.trim().toLowerCase();

        // Le rôle est en FetchType.EAGER : il est déjà chargé quand la transaction se ferme.
        User user = Tx.call(() -> users.findByEmail(normalized).orElse(null));

        // Si l'email est inconnu, on vérifie quand même un hash factice :
        // la réponse prend le même temps, on ne peut pas deviner quels emails existent.
        String hash = (user != null) ? user.getPassword() : DUMMY_HASH;
        boolean passwordOk = BCrypt.checkpw(password, hash);

        if (user == null || !passwordOk) {
            return Optional.empty();
        }
        return Optional.of(SessionUser.from(user));
    }
}
