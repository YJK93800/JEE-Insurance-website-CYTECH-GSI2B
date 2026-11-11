package fr.cytech.assurance.service;

import fr.cytech.assurance.dao.UserDao;
import fr.cytech.assurance.dao.impl.UserDaoImpl;
import fr.cytech.assurance.dto.request.RegisterRequest;
import fr.cytech.assurance.model.User;
import fr.cytech.assurance.model.enums.Role;
import fr.cytech.assurance.util.Tx;
import org.mindrot.jbcrypt.BCrypt;

public class AuthService {

    private final UserDao users;

    public AuthService() {
        this(new UserDaoImpl());
    }

    // permet de passer un autre DAO (utile pour les tests)
    public AuthService(UserDao users) {
        this.users = users;
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
            user.setRole(Role.CLIENT); // une inscription crée toujours un client

            return users.save(user);
        });
    }
}
