package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.RoleDao;
import fr.cytech.assurance.dao.UserDao;
import fr.cytech.assurance.dao.impl.RoleDaoImpl;
import fr.cytech.assurance.dao.impl.UserDaoImpl;
import fr.cytech.assurance.dto.request.RegisterRequest;
import fr.cytech.assurance.model.User;
import fr.cytech.assurance.model.Role;
import fr.cytech.assurance.util.Tx;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

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
}
